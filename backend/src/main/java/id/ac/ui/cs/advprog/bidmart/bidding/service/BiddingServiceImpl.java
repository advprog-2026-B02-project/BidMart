package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.client.CatalogClient;
import id.ac.ui.cs.advprog.bidmart.bidding.client.UserClient;
import id.ac.ui.cs.advprog.bidmart.bidding.client.WalletClient;
import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.WinnerDeterminedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.model.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.model.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.model.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.model.BidStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BiddingServiceImpl implements BiddingService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WalletClient walletClient;
    private final ApplicationEventPublisher eventPublisher;
    private final CatalogClient catalogClient;
    private final UserClient userClient;

    private static final String AUCTION_NOT_FOUND_MSG = "lelang tidak ditemukan";

    // anotasi transactional menjamin pessimistic lock bekerja dan rollback otomatis jika gagal
    @Override
    @Transactional
    public BidResponseDTO placeBid(UUID auctionId, UUID bidderId, BidRequestDTO requestDTO) {
        // fetch data dengan pessimistic lock untuk mencegah race condition
        Auction auction = auctionRepository.findByIdWithPessimisticLock(auctionId)
                .orElseThrow(() -> new IllegalArgumentException(AUCTION_NOT_FOUND_MSG));

        // pastikan user asli
        userClient.validateUser(bidderId);

        // penjual dilarang ngebid barangnya sendiri
        UUID sellerId = catalogClient.getSellerId(auction.getListingId());
        if (bidderId.equals(sellerId)) {
            throw new IllegalStateException("penjual tidak boleh menawar barangnya sendiri");
        }

        LocalDateTime now = LocalDateTime.now();

        // validasi status dan waktu lelang
        validateAuctionIsActive(auction, now);

        // amount yang dikirim user sekarang dianggap sebagai max amount (batas maksimal mereka)
        BigDecimal incomingMaxAmount = requestDTO.getAmount();
        validateBidAmount(auction, incomingMaxAmount);

        // tahan dana sebesar max amount
        UUID holdId = walletClient.holdFunds(bidderId, auctionId, incomingMaxAmount);

        try {
            // eksekusi logika perpanjangan waktu
            handleAntiSniping(auction, now);

            // ambil batas max penawar tertinggi saat ini (kalau belum ada anggap 0)
            BigDecimal currentMaxAmount = auction.getHighestBidderMaxAmount() != null
                    ? auction.getHighestBidderMaxAmount()
                    : BigDecimal.ZERO;

            BigDecimal previousPrice = auction.getCurrentPrice();
            Bid newBid;

            // Logika if-else utama dipisah ke helper method
            if (incomingMaxAmount.compareTo(currentMaxAmount) > 0) {
                newBid = processWinningBid(auction, bidderId, incomingMaxAmount, currentMaxAmount, holdId, now, sellerId);
            } else {
                newBid = processLosingProxyBid(auction, bidderId, incomingMaxAmount, currentMaxAmount, holdId, now, sellerId);
            }

            auctionRepository.save(auction);
            return buildResponse(newBid, auction, previousPrice);

        } catch (Exception e) {
            walletClient.releaseFunds(holdId);
            throw e;
        }
    }

    private Bid processWinningBid(Auction auction, UUID bidderId, BigDecimal incomingMaxAmount, BigDecimal currentMaxAmount, UUID holdId, LocalDateTime now, UUID sellerId) {
        BigDecimal increment = auction.getMinimumIncrement();
        BigDecimal newCurrentPrice;

        if (auction.getHighestBidderId() == null) {
            // lelang masih kosong, harga stay di pembukaan atau start price
            newCurrentPrice = auction.getCurrentPrice();
        } else {
            // outbid penawar lama, harga baru = max lama + increment
            newCurrentPrice = currentMaxAmount.add(increment);
            // cegah harga melebihi batas max penawar baru
            if (newCurrentPrice.compareTo(incomingMaxAmount) > 0) {
                newCurrentPrice = incomingMaxAmount;
            }
        }

        // catat data lama untuk event pelepasan dana
        UUID previousBidderId = auction.getHighestBidderId();
        UUID outbidHoldId = auction.getHighestBidderHoldId();

        // perbarui state lelang ke penawar baru
        auction.setCurrentPrice(newCurrentPrice);
        auction.setHighestBidderId(bidderId);
        auction.setHighestBidderHoldId(holdId);
        auction.setHighestBidderMaxAmount(incomingMaxAmount);
        auction.setBidCount(auction.getBidCount() + 1);

        if (newCurrentPrice.compareTo(auction.getReservePrice()) >= 0) {
            auction.setReserveMet(true);
        }

        Bid newBid = new Bid();
        newBid.setAuction(auction);
        newBid.setBidderId(bidderId);
        newBid.setAmount(newCurrentPrice);
        newBid.setStatus(BidStatus.ACCEPTED);
        newBid.setHoldId(holdId);
        newBid.setCreatedAt(now);
        newBid = bidRepository.save(newBid);

        if (previousBidderId != null) {
            eventPublisher.publishEvent(new BidPlacedEvent(auction.getId(), sellerId, bidderId, newCurrentPrice, previousBidderId, outbidHoldId));
        } else {
            eventPublisher.publishEvent(new BidPlacedEvent(auction.getId(), sellerId, bidderId, newCurrentPrice, null, null));
        }

        return newBid;
    }

    private Bid processLosingProxyBid(Auction auction, UUID bidderId, BigDecimal incomingMaxAmount, BigDecimal currentMaxAmount, UUID holdId, LocalDateTime now, UUID sellerId) {
        BigDecimal increment = auction.getMinimumIncrement();
        BigDecimal newCurrentPrice = incomingMaxAmount.add(increment);

        // ga boleh melebihi batas max penawar lama
        if (newCurrentPrice.compareTo(currentMaxAmount) > 0) {
            newCurrentPrice = currentMaxAmount;
        }

        auction.setCurrentPrice(newCurrentPrice);
        auction.setBidCount(auction.getBidCount() + 1);

        if (newCurrentPrice.compareTo(auction.getReservePrice()) >= 0) {
            auction.setReserveMet(true);
        }

        Bid newBid = new Bid();
        newBid.setAuction(auction);
        newBid.setBidderId(bidderId);
        newBid.setAmount(incomingMaxAmount);
        newBid.setStatus(BidStatus.OUTBID);
        newBid.setHoldId(holdId);
        newBid.setCreatedAt(now);
        newBid = bidRepository.save(newBid);

        // lepas dana penawar baru detik itu juga karena dia langsung kalah
        walletClient.releaseFunds(holdId);

        // penawar lama ga perlu ditahan dananya lagi karena dari awal udah ditahan full max

        // broadcast update harga baru ke websocket (tanpa outbid id karena pemenangnya tetep sama)
        eventPublisher.publishEvent(new BidPlacedEvent(auction.getId(), sellerId, auction.getHighestBidderId(), newCurrentPrice, null, null));

        return newBid;
    }

    private void validateAuctionIsActive(Auction auction, LocalDateTime now) {
        if (auction.getStatus() != AuctionStatus.ACTIVE && auction.getStatus() != AuctionStatus.EXTENDED) {
            throw new IllegalStateException("lelang tidak sedang berlangsung");
        }
        if (now.isAfter(auction.getEndTime())) {
            throw new IllegalStateException("waktu lelang telah berakhir");
        }
    }

    private void validateBidAmount(Auction auction, BigDecimal bidAmount) {
        BigDecimal minimumRequired = auction.getCurrentPrice().add(auction.getMinimumIncrement());
        if (bidAmount.compareTo(minimumRequired) < 0) {
            throw new IllegalArgumentException("penawaran terlalu rendah");
        }
    }

    private void handleAntiSniping(Auction auction, LocalDateTime now) {
        LocalDateTime timeThreshold = auction.getEndTime().minusMinutes(2);

        // jika bid masuk pada 2 menit terakhir, perpanjang waktu lelang
        if (now.isAfter(timeThreshold) || now.isEqual(timeThreshold)) {
            auction.setEndTime(now.plusMinutes(2));
            auction.setExtensionCount(auction.getExtensionCount() + 1);
            auction.setStatus(AuctionStatus.EXTENDED);
        }
    }

    private BidResponseDTO buildResponse(Bid bid, Auction auction, BigDecimal previousPrice) {
        return BidResponseDTO.builder().id(bid.getId()).auctionId(auction.getId()).bidderId(bid.getBidderId()).amount(bid.getAmount()).status(bid.getStatus().name()).holdId(bid.getHoldId()).previousHighBid(previousPrice).isNewHighBid(true).auctionEndTime(auction.getEndTime()).createdAt(bid.getCreatedAt()).build();
    }

    @Override
    @Transactional
    public AuctionResponseDTO startAuction(UUID listingId, AuctionStartRequestDTO request) {
        // cek barang valid atau gk
        catalogClient.validateListing(listingId);

        LocalDateTime now = LocalDateTime.now();

        Auction auction = new Auction();
        auction.setListingId(listingId);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentPrice(request.getStartPrice());
        auction.setMinimumIncrement(request.getMinimumIncrement());
        auction.setReservePrice(request.getReservePrice());
        auction.setStartTime(now);
        auction.setEndTime(request.getEndTime());
        auction.setOriginalEndTime(request.getEndTime());

        // harusnya pasti false karena belum ada yang ngebid
        auction.setReserveMet(false);

        auction = auctionRepository.save(auction);
        return mapToAuctionResponse(auction);
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponseDTO getAuctionStatus(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(() -> new IllegalArgumentException(AUCTION_NOT_FOUND_MSG));
        return mapToAuctionResponse(auction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BidResponseDTO> getBidHistory(UUID auctionId, Pageable pageable) {
        // mengubah list menjadi page untuk mendukung paginasi riwayat penawaran
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId, pageable).map(bid -> BidResponseDTO.builder().id(bid.getId()).bidderId(bid.getBidderId()).amount(bid.getAmount()).createdAt(bid.getCreatedAt()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResultDTO getAuctionResult(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(() -> new IllegalArgumentException(AUCTION_NOT_FOUND_MSG));

        if (auction.getStatus() == AuctionStatus.ACTIVE || auction.getStatus() == AuctionStatus.EXTENDED) {
            throw new IllegalStateException("lelang masih berlangsung");
        }

        return AuctionResultDTO.builder().auctionId(auction.getId()).status(auction.getStatus().name()).winnerId(auction.getHighestBidderId()).winningBid(auction.getCurrentPrice()).reserveMet(auction.getReserveMet()).closedAt(auction.getEndTime()).build();
    }

    // helper method untuk memetakan entity ke dto
    private AuctionResponseDTO mapToAuctionResponse(Auction auction) {
        BigDecimal nextBid = auction.getCurrentPrice().add(auction.getMinimumIncrement());

        return AuctionResponseDTO.builder().id(auction.getId()).listingId(auction.getListingId()).status(auction.getStatus().name()).currentPrice(auction.getCurrentPrice()).minimumNextBid(nextBid).highestBidderId(auction.getHighestBidderId()).startTime(auction.getStartTime()).endTime(auction.getEndTime()).originalEndTime(auction.getOriginalEndTime()).extensionCount(auction.getExtensionCount()).reserveMet(auction.getReserveMet()).build();
    }

    @Override
    @Transactional
    public void closeExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> expiredAuctions = auctionRepository.findExpiredActiveAuctions(now);

        for (Auction auction : expiredAuctions) {
            boolean hasBidder = auction.getHighestBidderId() != null;
            boolean isReserveMet = hasBidder && auction.getCurrentPrice().compareTo(auction.getReservePrice()) >= 0;

            if (isReserveMet) {
                auction.setStatus(AuctionStatus.WON);
                auction.setReserveMet(true);

                // publish event lelang dimenangkan agar modul wallet memotong dana pemenang
                eventPublisher.publishEvent(new WinnerDeterminedEvent(
                        auction.getId(),
                        auction.getHighestBidderId(),
                        auction.getCurrentPrice()
                ));
            } else {
                auction.setStatus(AuctionStatus.UNSOLD);
                auction.setReserveMet(false);

                // get seller id for event
                UUID sellerId = catalogClient.getSellerId(auction.getListingId());

                // publish event lelang gagal agar modul wallet melepas dana penawar tertinggi
                eventPublisher.publishEvent(new AuctionUnsoldEvent(auction.getId(), sellerId));
            }

            auctionRepository.save(auction);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BidResponseDTO> getUserBids(UUID userId, Pageable pageable) {
        // ambil data mentah dari database
        Page<Bid> userBids = bidRepository.findByBidderId(userId, pageable);

        // konversi entity bid menjadi bentuk dto
        return userBids.map(bid -> BidResponseDTO.builder()
                .id(bid.getId())
                .auctionId(bid.getAuction().getId())
                .bidderId(bid.getBidderId())
                .amount(bid.getAmount())
                .status(bid.getStatus().name())
                .holdId(bid.getHoldId())
                .isNewHighBid(false) // karena ini riwayat, kita default ke false
                .auctionEndTime(bid.getAuction().getEndTime()) // ambil dari relasi auction
                .createdAt(bid.getCreatedAt())
                .build());
    }
}
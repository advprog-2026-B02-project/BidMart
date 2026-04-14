package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.client.WalletClient;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.BidRequestDTO;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.BidResponseDTO;
import id.ac.ui.cs.advprog.bidmart.bidding.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.event.AuctionWonEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.event.BidPlacedEvent;
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

    // anotasi transactional menjamin pessimistic lock bekerja dan rollback otomatis jika gagal
    @Override
    @Transactional
    public BidResponseDTO placeBid(UUID auctionId, UUID bidderId, BidRequestDTO requestDTO) {
        // fetch data dengan pessimistic lock untuk mencegah race condition
        Auction auction = auctionRepository.findByIdWithPessimisticLock(auctionId).orElseThrow(() -> new IllegalArgumentException("lelang tidak ditemukan"));

        LocalDateTime now = LocalDateTime.now();

        // validasi status dan waktu lelang
        validateAuctionIsActive(auction, now);

        // validasi nominal penawaran
        validateBidAmount(auction, requestDTO.getAmount());

        // simpan data bidder sebelumnya untuk keperluan release dana
        UUID previousBidderId = auction.getHighestBidderId();
        BigDecimal previousPrice = auction.getCurrentPrice();
        UUID outbidHoldId = auction.getHighestBidderHoldId();

        // interaksi dengan modul wallet secara sinkronus untuk menahan dana
        UUID holdId = walletClient.holdFunds(bidderId, auctionId, requestDTO.getAmount());

        try {
            // eksekusi logika perpanjangan waktu
            handleAntiSniping(auction, now);

            // perbarui state lelang beserta hold id dari penawar terbaru
            auction.setCurrentPrice(requestDTO.getAmount());
            auction.setHighestBidderId(bidderId);
            auction.setHighestBidderHoldId(holdId);
            auction.setBidCount(auction.getBidCount() + 1);
            auctionRepository.save(auction);

            // catat riwayat penawaran ke database
            Bid newBid = new Bid();
            newBid.setAuction(auction);
            newBid.setBidderId(bidderId);
            newBid.setAmount(requestDTO.getAmount());
            newBid.setStatus(BidStatus.ACCEPTED);
            newBid.setHoldId(holdId);
            newBid.setCreatedAt(now);
            newBid = bidRepository.save(newBid);

            // publish event untuk memicu proses asinkronus seperti pelepasan dana penawar lama
            eventPublisher.publishEvent(new BidPlacedEvent(auctionId, bidderId, requestDTO.getAmount(), previousBidderId, outbidHoldId));

            return buildResponse(newBid, auction, previousPrice);

        } catch (Exception e) {
            // rollback dana jika terjadi kegagalan sistem pada database
            walletClient.releaseFunds(holdId);
            throw e;
        }
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

        // catatan: reserve price idealnya disimpan di entity auction atau ditarik dari catalog
        auction.setReserveMet(false);

        auction = auctionRepository.save(auction);
        return mapToAuctionResponse(auction);
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponseDTO getAuctionStatus(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(() -> new IllegalArgumentException("lelang tidak ditemukan"));
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
        Auction auction = auctionRepository.findById(auctionId).orElseThrow(() -> new IllegalArgumentException("lelang tidak ditemukan"));

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
                eventPublisher.publishEvent(new AuctionWonEvent(
                        auction.getId(),
                        auction.getHighestBidderId(),
                        auction.getCurrentPrice()
                ));
            } else {
                auction.setStatus(AuctionStatus.UNSOLD);
                auction.setReserveMet(false);

                // publish event lelang gagal agar modul wallet melepas dana penawar tertinggi
                eventPublisher.publishEvent(new AuctionUnsoldEvent(auction.getId()));
            }

            auctionRepository.save(auction);
        }
    }
}
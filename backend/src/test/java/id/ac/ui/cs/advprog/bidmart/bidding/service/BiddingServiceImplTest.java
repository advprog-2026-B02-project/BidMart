package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.client.CatalogClient;
import id.ac.ui.cs.advprog.bidmart.bidding.client.WalletClient;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.*;
import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.WinnerDeterminedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.model.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.model.AuctionStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.model.Bid;
import id.ac.ui.cs.advprog.bidmart.bidding.model.BidStatus;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingServiceImplTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private WalletClient walletClient;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private BiddingServiceImpl biddingService;

    private Auction auction;
    private UUID auctionId;
    private UUID bidderId;
    private UUID oldBidderId;
    private UUID holdId;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        oldBidderId = UUID.randomUUID();
        holdId = UUID.randomUUID();

        auction = new Auction();
        auction.setId(auctionId);
        auction.setListingId(UUID.randomUUID());
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setCurrentPrice(new BigDecimal("100000"));
        auction.setMinimumIncrement(new BigDecimal("10000"));
        auction.setReservePrice(new BigDecimal("500000"));
        auction.setStartTime(LocalDateTime.now().minusDays(1));
        auction.setEndTime(LocalDateTime.now().plusDays(1));
        auction.setOriginalEndTime(auction.getEndTime());
        auction.setBidCount(0);
        auction.setExtensionCount(0);
        auction.setReserveMet(false);
    }

    // test place bid skenario penawar baru menang
    @Test
    void placeBid_Success_NewBidderWins_FirstBid() {
        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("150000"));

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(bidderId, auctionId, request.getAmount())).thenReturn(holdId);

        Bid savedBid = new Bid();
        savedBid.setId(UUID.randomUUID());
        savedBid.setAmount(new BigDecimal("100000")); // karena bid pertama, harga stay di currentPrice
        savedBid.setStatus(BidStatus.ACCEPTED);
        savedBid.setCreatedAt(LocalDateTime.now());
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

        BidResponseDTO response = biddingService.placeBid(auctionId, bidderId, request);

        assertNotNull(response);
        assertEquals(BidStatus.ACCEPTED.name(), response.getStatus());
        assertEquals(bidderId, auction.getHighestBidderId());
        assertEquals(new BigDecimal("150000"), auction.getHighestBidderMaxAmount());

        // verifikasi event dipublish tanpa outbid id
        verify(eventPublisher).publishEvent(any(BidPlacedEvent.class));
    }

    @Test
    void placeBid_Success_NewBidderOutbidsOldBidder() {
        // setup old bidder dengan max 150rb
        auction.setHighestBidderId(oldBidderId);
        auction.setHighestBidderHoldId(UUID.randomUUID());
        auction.setHighestBidderMaxAmount(new BigDecimal("150000"));

        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("200000")); // new bidder max 200rb

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(bidderId, auctionId, request.getAmount())).thenReturn(holdId);

        Bid savedBid = new Bid();
        savedBid.setId(UUID.randomUUID());
        savedBid.setStatus(BidStatus.ACCEPTED);
        savedBid.setCreatedAt(LocalDateTime.now());
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

        biddingService.placeBid(auctionId, bidderId, request);

        // harga baru harusnya = max lama (150k) + increment (10k) = 160k
        assertEquals(new BigDecimal("160000"), auction.getCurrentPrice());
        assertEquals(bidderId, auction.getHighestBidderId());

        // tangkap event yang dipublish pakai ArgumentCaptor
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        // verify isi event-nya
        assertInstanceOf(BidPlacedEvent.class, eventCaptor.getValue());
        BidPlacedEvent publishedEvent = (BidPlacedEvent) eventCaptor.getValue();
        assertEquals(oldBidderId, publishedEvent.outbidUserId());
    }

    // test place bid skenario auto-bid (penawar lama bertahan)
    @Test
    void placeBid_Success_OldBidderDefendsViaProxy() {
        // setup old bidder dengan max 500rb
        auction.setHighestBidderId(oldBidderId);
        auction.setHighestBidderHoldId(UUID.randomUUID());
        auction.setHighestBidderMaxAmount(new BigDecimal("500000"));

        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("200000")); // iseng nawar 200rb

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(bidderId, auctionId, request.getAmount())).thenReturn(holdId);

        Bid savedBid = new Bid();
        savedBid.setId(UUID.randomUUID());
        savedBid.setStatus(BidStatus.OUTBID);
        savedBid.setCreatedAt(LocalDateTime.now());
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

        biddingService.placeBid(auctionId, bidderId, request);

        // highest bidder harus tetep oldBidderId
        assertEquals(oldBidderId, auction.getHighestBidderId());

        // harga baru harusnya = incoming max (200k) + increment (10k) = 210k
        assertEquals(new BigDecimal("210000"), auction.getCurrentPrice());

        // pastikan duit bidder baru langsung dilepas
        verify(walletClient).releaseFunds(holdId);
    }

    // test validasi & exception
    @Test
    void placeBid_Fail_AuctionNotFound() {
        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                biddingService.placeBid(auctionId, bidderId, new BidRequestDTO())
        );
    }

    @Test
    void placeBid_Fail_AuctionEnded() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(5)); // udah abis
        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));

        assertThrows(IllegalStateException.class, () ->
                biddingService.placeBid(auctionId, bidderId, new BidRequestDTO())
        );
    }

    @Test
    void placeBid_Fail_AmountTooLow() {
        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("105000")); // kurang dari current(100k) + increment(10k)

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));

        assertThrows(IllegalArgumentException.class, () ->
                biddingService.placeBid(auctionId, bidderId, request)
        );
    }

    @Test
    void placeBid_Fail_RollbackOnDatabaseError() {
        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("150000"));

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(bidderId, auctionId, request.getAmount())).thenReturn(holdId);

        // simulasi db mati pas nyimpen
        when(bidRepository.save(any(Bid.class))).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () ->
                biddingService.placeBid(auctionId, bidderId, request)
        );

        // pastikan duit dilepas lagi gara-gara db error
        verify(walletClient).releaseFunds(holdId);
    }

    @Test
    void placeBid_Success_AntiSnipingTriggered() {
        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("150000"));

        // set sisa waktu tinggal 1 menit
        auction.setEndTime(LocalDateTime.now().plusMinutes(1));

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(bidderId, auctionId, request.getAmount())).thenReturn(holdId);

        Bid savedBid = new Bid();
        savedBid.setId(UUID.randomUUID());
        savedBid.setStatus(BidStatus.ACCEPTED);
        savedBid.setCreatedAt(LocalDateTime.now());
        when(bidRepository.save(any(Bid.class))).thenReturn(savedBid);

        biddingService.placeBid(auctionId, bidderId, request);

        // pastikan waktu nambah dan status jadi extended
        assertEquals(AuctionStatus.EXTENDED, auction.getStatus());
        assertEquals(1, auction.getExtensionCount());
    }

    @Test
    void startAuction_Success() {
        AuctionStartRequestDTO request = new AuctionStartRequestDTO();
        request.setStartPrice(new BigDecimal("100000"));
        request.setMinimumIncrement(new BigDecimal("10000"));
        request.setReservePrice(new BigDecimal("500000"));
        request.setEndTime(LocalDateTime.now().plusDays(1));

        doNothing().when(catalogClient).validateListing(any(UUID.class));

        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArguments()[0]);

        AuctionResponseDTO response = biddingService.startAuction(UUID.randomUUID(), request);

        assertNotNull(response);
        assertFalse(response.getReserveMet());
        assertEquals(AuctionStatus.ACTIVE.name(), response.getStatus());
    }

    @Test
    void getAuctionStatus_Success() {
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        AuctionResponseDTO response = biddingService.getAuctionStatus(auctionId);

        assertNotNull(response);
        assertEquals(auction.getCurrentPrice(), response.getCurrentPrice());
    }

    @Test
    void getBidHistory_Success() {
        Page<Bid> mockPage = new PageImpl<>(List.of(new Bid()));
        when(bidRepository.findByAuctionIdOrderByAmountDesc(eq(auctionId), any(PageRequest.class)))
                .thenReturn(mockPage);

        Page<BidResponseDTO> result = biddingService.getBidHistory(auctionId, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAuctionResult_Fail_AuctionStillActive() {
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        assertThrows(IllegalStateException.class, () ->
                biddingService.getAuctionResult(auctionId)
        );
    }

    // test scheduler logic
    @Test
    void closeExpiredAuctions_Success_ReserveMet() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(5));
        auction.setHighestBidderId(bidderId);
        auction.setCurrentPrice(new BigDecimal("600000")); // lewatin reserve (500k)

        when(auctionRepository.findExpiredActiveAuctions(any(LocalDateTime.class)))
                .thenReturn(List.of(auction));

        biddingService.closeExpiredAuctions();

        assertEquals(AuctionStatus.WON, auction.getStatus());
        assertTrue(auction.getReserveMet());

        verify(auctionRepository).save(auction);
        verify(eventPublisher).publishEvent(any(WinnerDeterminedEvent.class));
    }

    @Test
    void closeExpiredAuctions_Success_ReserveNotMet() {
        auction.setEndTime(LocalDateTime.now().minusMinutes(5));
        auction.setHighestBidderId(bidderId);
        auction.setCurrentPrice(new BigDecimal("150000")); // ga ngelewatin reserve (500k)

        when(auctionRepository.findExpiredActiveAuctions(any(LocalDateTime.class)))
                .thenReturn(List.of(auction));

        biddingService.closeExpiredAuctions();

        assertEquals(AuctionStatus.UNSOLD, auction.getStatus());
        assertFalse(auction.getReserveMet());

        verify(auctionRepository).save(auction);
        verify(eventPublisher).publishEvent(any(AuctionUnsoldEvent.class));
    }

    @Test
    void placeBid_ProxyCap_Winning_Flow() {
        // harga sekarang 100k, increment 10k.
        auction.setCurrentPrice(new BigDecimal("100000"));
        auction.setMinimumIncrement(new BigDecimal("10000"));

        // penawar lama punya max 150k
        auction.setHighestBidderId(oldBidderId);
        auction.setHighestBidderMaxAmount(new BigDecimal("150000"));
        auction.setHighestBidderHoldId(UUID.randomUUID());

        // penawar baru masukin limit 155k (Cuma beda 5k dari max lama)
        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("155000"));

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(any(), any(), any())).thenReturn(holdId);
        when(bidRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        biddingService.placeBid(auctionId, bidderId, request);

        // 150k + 10k = 160k
        // tapi karena limit baru cuma 155k, maka harga harusnya mentok di 155k
        assertEquals(0, new BigDecimal("155000").compareTo(auction.getCurrentPrice()));
    }

    @Test
    void placeBid_ReserveMet_Flow() {
        // set reservePrice di 100k
        auction.setCurrentPrice(new BigDecimal("100000"));
        auction.setMinimumIncrement(new BigDecimal("10000"));
        auction.setReservePrice(new BigDecimal("100000")); // reserve diset sama dengan harga start
        auction.setHighestBidderId(null);

        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("250000"));

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(any(), any(), any())).thenReturn(holdId);
        when(bidRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        biddingService.placeBid(auctionId, bidderId, request);

        // newCurrentPrice (100k) >= reservePrice (100k) -> harusnya true
        assertTrue(auction.getReserveMet());
        assertEquals(0, new BigDecimal("100000").compareTo(auction.getCurrentPrice()));
    }

    @Test
    void placeBid_LosingProxy_But_ReserveIsMet() {
        auction.setCurrentPrice(new BigDecimal("100000"));
        auction.setMinimumIncrement(new BigDecimal("10000"));
        auction.setReservePrice(new BigDecimal("150000")); // reserve di 150k

        // penawar lama limitnya tinggi banget
        auction.setHighestBidderId(oldBidderId);
        auction.setHighestBidderMaxAmount(new BigDecimal("500000")); // limit 500k
        auction.setHighestBidderHoldId(UUID.randomUUID());

        // penawar baru masuk dengan angka yang ngelewatin reserve tapi tetep kalah ama penawar lama
        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("160000")); // 160k > 150k (reserve), tapi < 500k (kalah)

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));
        when(walletClient.holdFunds(any(), any(), any())).thenReturn(holdId);
        when(bidRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        biddingService.placeBid(auctionId, bidderId, request);

        // meskipun kalah (OUTBID), harga lelang naik dan reserve price terpenuhi
        assertTrue(auction.getReserveMet());

        // harga baru harusnya 160k + 10k = 170k
        assertEquals(0, new BigDecimal("170000").compareTo(auction.getCurrentPrice()));
    }

    @Test
    void placeBid_LosingProxy_CapAtMaxAmount() {
        // current 100k, Increment 10k.
        auction.setCurrentPrice(new BigDecimal("100000"));
        auction.setHighestBidderMaxAmount(new BigDecimal("200000")); // max lama 200k
        auction.setHighestBidderId(oldBidderId);

        BidRequestDTO request = new BidRequestDTO();
        request.setAmount(new BigDecimal("195000")); // bidder baru nawar 195k (kalah)

        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));

        // harus di-mock biar holdId ga null pas mau di-release
        when(walletClient.holdFunds(any(), any(), any())).thenReturn(holdId);
        when(bidRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        biddingService.placeBid(auctionId, bidderId, request);

        // harga naik jadi 200k (max penawar lama), bukan 205k (195k + 10k) karena mentok
        assertEquals(new BigDecimal("200000"), auction.getCurrentPrice());
        verify(walletClient).releaseFunds(holdId);
    }

    @Test
    void validateAuctionIsActive_ThrowsWhenNotActive() {
        auction.setStatus(AuctionStatus.WON);
        when(auctionRepository.findByIdWithPessimisticLock(auctionId)).thenReturn(Optional.of(auction));

        assertThrows(IllegalStateException.class, () ->
                biddingService.placeBid(auctionId, bidderId, new BidRequestDTO())
        );
    }

    @Test
    void getAuctionResult_Coverage_ReturnStatement() {
        // lmao
        auction.setStatus(AuctionStatus.WON);
        auction.setHighestBidderId(bidderId);
        auction.setReserveMet(true);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        AuctionResultDTO result = biddingService.getAuctionResult(auctionId);

        assertNotNull(result);
        assertEquals(auctionId, result.getAuctionId());
    }
}
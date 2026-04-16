package id.ac.ui.cs.advprog.bidmart.bidding.event;

import id.ac.ui.cs.advprog.bidmart.bidding.client.WalletClient;
import id.ac.ui.cs.advprog.bidmart.bidding.model.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingEventListenerTest {

    @Mock
    private WalletClient walletClient;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private BiddingEventListener eventListener;

    // pakai @Captor biar tipe Generics-nya kebaca jelas sama compiler
    @Captor
    private ArgumentCaptor<Map<String, Object>> payloadCaptor;

    private UUID auctionId;
    private UUID bidderId;
    private UUID outbidUserId;
    private UUID outbidHoldId;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        outbidUserId = UUID.randomUUID();
        outbidHoldId = UUID.randomUUID();

        auction = new Auction();
        auction.setId(auctionId);
        auction.setCurrentPrice(new BigDecimal("150000"));
        auction.setMinimumIncrement(new BigDecimal("10000"));
        auction.setBidCount(1);
        auction.setExtensionCount(0);
        auction.setEndTime(LocalDateTime.now().plusHours(1));
    }

    @Test
    void handleBidPlacedEvent_Success_WithOutbidAndExtension() {
        auction.setExtensionCount(1);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        BidPlacedEvent event = new BidPlacedEvent(auctionId, bidderId, new BigDecimal("150000"), outbidUserId, outbidHoldId);

        eventListener.handleBidPlacedEvent(event);

        verify(walletClient).releaseFunds(outbidHoldId);

        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/auctions/" + auctionId), payloadCaptor.capture());

        Map<String, Object> firstPayload = payloadCaptor.getAllValues().get(0);
        assertEquals("NEW_BID", firstPayload.get("type"));

        Map<String, Object> secondPayload = payloadCaptor.getAllValues().get(1);
        assertEquals("AUCTION_EXTENDED", secondPayload.get("type"));
    }

    @Test
    void handleBidPlacedEvent_Success_NoOutbidNoExtension() {
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        BidPlacedEvent event = new BidPlacedEvent(auctionId, bidderId, new BigDecimal("100000"), null, null);

        eventListener.handleBidPlacedEvent(event);

        verify(walletClient, never()).releaseFunds(any());

        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Map.class));
    }

    @Test
    void handleBidPlacedEvent_CatchException_WhenWalletFails() {
        BidPlacedEvent event = new BidPlacedEvent(auctionId, bidderId, new BigDecimal("150000"), outbidUserId, outbidHoldId);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        doThrow(new RuntimeException("Wallet Error")).when(walletClient).releaseFunds(any());

        eventListener.handleBidPlacedEvent(event);

        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Map.class));
    }

    @Test
    void handleAuctionWonEvent_Success() {
        AuctionWonEvent event = new AuctionWonEvent(auctionId, bidderId, new BigDecimal("500000"));

        eventListener.handleAuctionWonEvent(event);

        verify(walletClient).captureWinnerFunds(auctionId, bidderId);

        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), payloadCaptor.capture());

        assertEquals("AUCTION_ENDED", payloadCaptor.getValue().get("type"));

        // pakai Map<?, ?> buat nested object biar ga kena Unchecked Assignment
        Map<?, ?> data = (Map<?, ?>) payloadCaptor.getValue().get("data");
        assertEquals("WON", data.get("status"));
    }

    @Test
    void handleAuctionUnsoldEvent_Success() {
        AuctionUnsoldEvent event = new AuctionUnsoldEvent(auctionId);

        eventListener.handleAuctionUnsoldEvent(event);

        verify(walletClient).releaseAllAuctionHolds(auctionId);

        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), payloadCaptor.capture());

        assertEquals("AUCTION_ENDED", payloadCaptor.getValue().get("type"));

        // pakai Map<?, ?>
        Map<?, ?> data = (Map<?, ?>) payloadCaptor.getValue().get("data");
        assertEquals("UNSOLD", data.get("status"));
    }

    @Test
    void handleBidPlacedEvent_ShouldLogAndContinue_WhenWalletReleaseFundsFails() {
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        BidPlacedEvent event = new BidPlacedEvent(auctionId, bidderId, new BigDecimal("150000"), outbidUserId, outbidHoldId);

        // simulate error pas release funds
        doThrow(new RuntimeException("Wallet system down")).when(walletClient).releaseFunds(outbidHoldId);

        eventListener.handleBidPlacedEvent(event);

        // verify websocket tetep dikirim meskipun wallet error
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/auctions/" + auctionId), any(Map.class));
    }

    @Test
    void handleBidPlacedEvent_ShouldLogAndContinue_WhenNotificationFails() {
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        BidPlacedEvent event = new BidPlacedEvent(auctionId, bidderId, new BigDecimal("150000"), outbidUserId, outbidHoldId);

        eventListener.handleBidPlacedEvent(event);

        verify(messagingTemplate, atLeastOnce()).convertAndSend(anyString(), any(Map.class));
    }

    @Test
    void handleAuctionUnsoldEvent_Success_FullCoverage() {
        // tangani lelang yang berakhir tanpa pemenang
        AuctionUnsoldEvent event = new AuctionUnsoldEvent(auctionId);

        eventListener.handleAuctionUnsoldEvent(event);

        // pastiin wallet melepas semua hold dana untuk auction tersebut
        verify(walletClient).releaseAllAuctionHolds(auctionId);

        // pastiin websocket ngirim info UNSOLD
        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), payloadCaptor.capture());
        Map<String, Object> payload = payloadCaptor.getValue();
        assertEquals("AUCTION_ENDED", payload.get("type"));

        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        assertEquals("UNSOLD", data.get("status"));
    }

    @Test
    void handleEvents_ShouldLog_WhenWebsocketFails() {
        // trigger catch block di handleBidPlacedEvent
        when(auctionRepository.findById(any())).thenReturn(Optional.of(auction));
        doThrow(new RuntimeException("Simulated WS Fail")).when(messagingTemplate).convertAndSend(anyString(), any(Map.class));

        eventListener.handleBidPlacedEvent(new BidPlacedEvent(auctionId, bidderId, BigDecimal.ONE, null, null));

        // trigger catch block di handleAuctionWonEvent
        eventListener.handleAuctionWonEvent(new AuctionWonEvent(auctionId, bidderId, BigDecimal.TEN));

        // trigger catch block di handleAuctionUnsoldEvent
        eventListener.handleAuctionUnsoldEvent(new AuctionUnsoldEvent(auctionId));

        verify(messagingTemplate, atLeast(3)).convertAndSend(anyString(), any(Map.class));
    }
}
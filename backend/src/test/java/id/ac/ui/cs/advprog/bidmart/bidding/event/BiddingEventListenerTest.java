package id.ac.ui.cs.advprog.bidmart.bidding.event;

import id.ac.ui.cs.advprog.bidmart.bidding.model.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.WinnerDeterminedEvent;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingEventListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private BiddingEventListener eventListener;

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
    void handleBidPlacedEvent_Success_WithExtension() {
        auction.setExtensionCount(1);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        UUID sellerId = UUID.randomUUID();
        BidPlacedEvent event = new BidPlacedEvent(auctionId, sellerId, bidderId, new BigDecimal("150000"), outbidUserId, outbidHoldId);

        eventListener.handleBidPlacedEvent(event);

        // hanya verifikasi pengiriman WebSocket (2 kali karena ada extension)
        verify(messagingTemplate, times(2)).convertAndSend(eq("/topic/auctions/" + auctionId), payloadCaptor.capture());

        Map<String, Object> firstPayload = payloadCaptor.getAllValues().get(0);
        assertEquals("NEW_BID", firstPayload.get("type"));

        Map<String, Object> secondPayload = payloadCaptor.getAllValues().get(1);
        assertEquals("AUCTION_EXTENDED", secondPayload.get("type"));
    }

    @Test
    void handleBidPlacedEvent_Success_NoExtension() {
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        UUID sellerId = UUID.randomUUID();
        BidPlacedEvent event = new BidPlacedEvent(auctionId, sellerId, bidderId, new BigDecimal("100000"), null, null);

        eventListener.handleBidPlacedEvent(event);

        // hanya verifikasi pengiriman WebSocket 1 kali
        verify(messagingTemplate, times(1)).convertAndSend(anyString(), any(Map.class));
    }

    @Test
    void handleWinnerDeterminedEvent_Success() {
        WinnerDeterminedEvent event = new WinnerDeterminedEvent(auctionId, bidderId, new BigDecimal("500000"));
        eventListener.handleWinnerDeterminedEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), payloadCaptor.capture());

        assertEquals("AUCTION_ENDED", payloadCaptor.getValue().get("type"));

        // pakai Map<?, ?> buat nested object biar ga kena Unchecked Assignment
        Map<?, ?> data = (Map<?, ?>) payloadCaptor.getValue().get("data");
        assertEquals("WON", data.get("status"));
    }

    @Test
    void handleAuctionUnsoldEvent_Success() {
        UUID sellerId = UUID.randomUUID();
        AuctionUnsoldEvent event = new AuctionUnsoldEvent(auctionId, sellerId);

        eventListener.handleAuctionUnsoldEvent(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/auctions/" + auctionId), payloadCaptor.capture());

        assertEquals("AUCTION_ENDED", payloadCaptor.getValue().get("type"));

        Map<?, ?> data = (Map<?, ?>) payloadCaptor.getValue().get("data");
        assertEquals("UNSOLD", data.get("status"));
    }

    @Test
    void handleEvents_ShouldLog_WhenWebsocketFails() {
        when(auctionRepository.findById(any())).thenReturn(Optional.of(auction));

        // simulate error saat ngirim pesan WebSocket
        doThrow(new RuntimeException("Simulated WS Fail")).when(messagingTemplate).convertAndSend(anyString(), any(Map.class));

        // execute ketiga event
        eventListener.handleBidPlacedEvent(new BidPlacedEvent(auctionId, UUID.randomUUID(), bidderId, BigDecimal.ONE, null, null));
        eventListener.handleWinnerDeterminedEvent(new WinnerDeterminedEvent(auctionId, bidderId, BigDecimal.TEN));
        eventListener.handleAuctionUnsoldEvent(new AuctionUnsoldEvent(auctionId, UUID.randomUUID()));

        // pastiin methode convertAndSend tetap dipanggil 3 kali meski error (karena ada try-catch di tiap handler)
        verify(messagingTemplate, atLeast(3)).convertAndSend(anyString(), any(Map.class));
    }
}
package id.ac.ui.cs.advprog.bidmart.notifications.event;

import id.ac.ui.cs.advprog.bidmart.common.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.model.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.BidRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidPlacedEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @InjectMocks
    private BidPlacedEventListener listener;

    private UUID auctionId;
    private UUID sellerId;
    private UUID bidderId;
    private UUID outbidUserId;
    private UUID outbidHoldId;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        outbidUserId = UUID.randomUUID();
        outbidHoldId = UUID.randomUUID();

        auction = mock(Auction.class);
    }

    @Test
    void testOnBidPlaced_WithOutbid() {
        UUID sellerId = UUID.randomUUID();
        UUID otherBidderId = UUID.randomUUID();
        BidPlacedEvent event = new BidPlacedEvent(auctionId, sellerId, bidderId, new BigDecimal("150000"), outbidUserId, outbidHoldId);


        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findDistinctBidderIdsByAuctionId(auctionId)).thenReturn(List.of(bidderId, outbidUserId, otherBidderId));

        listener.onBidPlaced(event);

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
            notification.getUserId().equals(outbidUserId) &&
            notification.getType() == NotificationType.OUTBID
        ));

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
            notification.getUserId().equals(sellerId) &&
            notification.getType() == NotificationType.BID_PLACED
        ));

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
            notification.getUserId().equals(otherBidderId) &&
            notification.getType() == NotificationType.BID_PLACED
        ));
    }

    @Test
    void testOnBidPlaced_NoOutbid() {
        UUID otherBidderId = UUID.randomUUID();
        BidPlacedEvent event = new BidPlacedEvent(auctionId, sellerId, bidderId, new BigDecimal("150000"), null, null);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findDistinctBidderIdsByAuctionId(auctionId)).thenReturn(List.of(bidderId, otherBidderId));

        listener.onBidPlaced(event);

        verify(notificationService, never()).saveNotification(argThat(notification ->
            notification.getType() == NotificationType.OUTBID
        ));

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
            notification.getUserId().equals(sellerId) &&
            notification.getType() == NotificationType.BID_PLACED
        ));

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
            notification.getUserId().equals(otherBidderId) &&
            notification.getType() == NotificationType.BID_PLACED
        ));
    }
}
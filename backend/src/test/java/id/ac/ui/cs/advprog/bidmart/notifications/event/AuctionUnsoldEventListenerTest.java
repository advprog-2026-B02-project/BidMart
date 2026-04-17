package id.ac.ui.cs.advprog.bidmart.notifications.event;

import id.ac.ui.cs.advprog.bidmart.bidding.model.Auction;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionUnsoldEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private AuctionUnsoldEventListener listener;

    @Test
    void onAuctionUnsold_WhenAuctionExists_ShouldSaveAuctionLostNotification() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        AuctionUnsoldEvent event = new AuctionUnsoldEvent(auctionId, sellerId);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(new Auction()));

        listener.onAuctionUnsold(event);

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
                notification.getUserId().equals(sellerId)
                        && notification.getType() == NotificationType.AUCTION_LOST
                        && notification.getData() != null
                        && auctionId.equals(notification.getData().get("auctionId"))
        ));
    }

    @Test
    void onAuctionUnsold_WhenAuctionNotFound_ShouldNotSaveNotification() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        AuctionUnsoldEvent event = new AuctionUnsoldEvent(auctionId, sellerId);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        listener.onAuctionUnsold(event);

        verify(notificationService, never()).saveNotification(argThat(notification -> true));
    }
}

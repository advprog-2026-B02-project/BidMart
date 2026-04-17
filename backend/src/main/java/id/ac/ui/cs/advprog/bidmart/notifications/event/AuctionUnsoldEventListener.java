package id.ac.ui.cs.advprog.bidmart.notifications.event;

import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionUnsoldEventListener {

    private final NotificationService notificationService;
    private final AuctionRepository auctionRepository;

    @EventListener
    public void onAuctionUnsold(AuctionUnsoldEvent event) {
        log.info("Processing auction unsold event for auction: {}", event.auctionId());

        auctionRepository.findById(event.auctionId()).ifPresent(auction -> {
            SaveNotification notification = SaveNotification.builder()
                    .userId(event.sellerId())
                    .type(NotificationType.AUCTION_LOST)
                    .title("Auction Ended Without Bids")
                    .message("Your auction has ended without any bids.")
                    .data(Map.of("auctionId", event.auctionId()))
                    .build();

            notificationService.saveNotification(notification);
        });
    }
}
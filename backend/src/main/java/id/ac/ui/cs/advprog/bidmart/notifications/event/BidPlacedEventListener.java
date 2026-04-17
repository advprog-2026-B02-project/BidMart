package id.ac.ui.cs.advprog.bidmart.notifications.event;

import id.ac.ui.cs.advprog.bidmart.common.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.BidRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidPlacedEventListener {

    private final NotificationService notificationService;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    @EventListener
    public void onBidPlaced(BidPlacedEvent event) {
        log.info("Processing bid placed event for auction: {}", event.auctionId());

        // Notify outbid user if any
        if (event.outbidUserId() != null) {
            SaveNotification outbidNotification = SaveNotification.builder()
                    .userId(event.outbidUserId())
                    .type(NotificationType.OUTBID)
                    .title("You've Been Outbid")
                    .message("Your bid has been surpassed by a higher bid of " + event.newBidAmount() + " on the auction.")
                    .data(Map.of("auctionId", event.auctionId(), "newBidAmount", event.newBidAmount()))
                    .build();
            notificationService.saveNotification(outbidNotification);
        }

        // Get seller from auction
        auctionRepository.findById(event.auctionId()).ifPresent(auction -> {
            // Notify seller
            SaveNotification sellerNotification = SaveNotification.builder()
                    .userId(event.sellerId())
                    .type(NotificationType.BID_PLACED)
                    .title("New Bid on Your Auction")
                    .message("Someone placed a bid of " + event.newBidAmount() + " on your auction.")
                    .data(Map.of("auctionId", event.auctionId(), "bidAmount", event.newBidAmount()))
                    .build();
            notificationService.saveNotification(sellerNotification);

            // Notify other bidders (all bidders except the new one and outbid one)
            List<UUID> bidderIds = bidRepository.findDistinctBidderIdsByAuctionId(event.auctionId());
            bidderIds.stream()
                    .filter(bidderId -> !bidderId.equals(event.newBidderId()) && !bidderId.equals(event.outbidUserId()))
                    .forEach(bidderId -> {
                        SaveNotification bidderNotification = SaveNotification.builder()
                                .userId(bidderId)
                                .type(NotificationType.BID_PLACED)
                                .title("New Bid Placed")
                                .message("A new bid of " + event.newBidAmount() + " was placed on an auction you're bidding on.")
                                .data(Map.of("auctionId", event.auctionId(), "bidAmount", event.newBidAmount()))
                                .build();
                        notificationService.saveNotification(bidderNotification);
                    });
        });
    }
}
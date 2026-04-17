package id.ac.ui.cs.advprog.bidmart.notifications.event;

import id.ac.ui.cs.advprog.bidmart.common.event.WinnerDeterminedEvent;
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
public class WinnerDeterminedEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onWinnerDetermined(WinnerDeterminedEvent event) {
        log.info("Processing winner determined event for auction: {}, winner: {}", event.auctionId(), event.winnerId());

        SaveNotification notification = SaveNotification.builder()
                .userId(event.winnerId())
                .type(NotificationType.AUCTION_WON)
                .title("Congratulations! You Won the Auction")
                .message("You have won the auction with a winning bid of " + event.winningAmount() + ".")
                .data(Map.of("auctionId", event.auctionId(), "winningAmount", event.winningAmount()))
                .build();

        notificationService.saveNotification(notification);
    }
}

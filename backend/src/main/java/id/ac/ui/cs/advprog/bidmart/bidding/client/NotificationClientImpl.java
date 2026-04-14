package id.ac.ui.cs.advprog.bidmart.bidding.client;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationClientImpl implements NotificationClient {

    private final NotificationService notificationService;

    @Override
    public void sendOutbidNotification(UUID userId, UUID auctionId) {
        SaveNotification notification = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.OUTBID)
                .title("penawaran terkalahkan")
                .message("penawaran anda pada sebuah lelang telah dilewati oleh pengguna lain.")
                .data(Map.of("auctionId", auctionId.toString()))
                .build();

        notificationService.saveNotification(notification);
    }

    @Override
    public void sendAuctionWonNotification(UUID userId, UUID auctionId) {
        SaveNotification notification = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.AUCTION_WON)
                .title("selamat! anda memenangkan lelang")
                .message("anda berhasil memenangkan lelang. dana yang ditahan telah dieksekusi menjadi pembayaran.")
                .data(Map.of("auctionId", auctionId.toString()))
                .build();

        notificationService.saveNotification(notification);
    }
}
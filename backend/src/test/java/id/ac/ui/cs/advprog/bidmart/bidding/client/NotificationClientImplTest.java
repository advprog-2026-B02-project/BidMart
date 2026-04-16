package id.ac.ui.cs.advprog.bidmart.bidding.client;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationClientImplTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationClientImpl notificationClient;

    @Captor
    private ArgumentCaptor<SaveNotification> notificationCaptor;

    private UUID userId;
    private UUID auctionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
    }

    @Test
    void sendOutbidNotification_Success() {
        notificationClient.sendOutbidNotification(userId, auctionId);

        // verify service dipanggil dan tangkap datanya
        verify(notificationService).saveNotification(notificationCaptor.capture());
        SaveNotification captured = notificationCaptor.getValue();

        // pastiin isi notifikasinya bener
        assertEquals(userId, captured.getUserId());
        assertEquals(NotificationType.OUTBID, captured.getType());
        assertEquals("penawaran terkalahkan", captured.getTitle());
        assertEquals(auctionId.toString(), captured.getData().get("auctionId"));
    }

    @Test
    void sendAuctionWonNotification_Success() {
        notificationClient.sendAuctionWonNotification(userId, auctionId);

        // verify service dipanggil dan tangkap datanya
        verify(notificationService).saveNotification(notificationCaptor.capture());
        SaveNotification captured = notificationCaptor.getValue();

        // pastiin isi notifikasinya bener
        assertEquals(userId, captured.getUserId());
        assertEquals(NotificationType.AUCTION_WON, captured.getType());
        assertEquals("selamat! anda memenangkan lelang", captured.getTitle());
        assertEquals(auctionId.toString(), captured.getData().get("auctionId"));
    }
}
package id.ac.ui.cs.advprog.bidmart.bidding.event;

import id.ac.ui.cs.advprog.bidmart.bidding.repository.AuctionRepository;
import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.BidPlacedEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.WinnerDeterminedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiddingEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuctionRepository auctionRepository;

    private static final String AUCTION_TOPIC_PREFIX = "/topic/auctions/";

    @Async
    @EventListener
    public void handleBidPlacedEvent(BidPlacedEvent event) {
        log.info("memproses event bid untuk websocket lelang: {}", event.auctionId());

        // broadcast realtime update ke semua user yang sedang melihat lelang ini
        try {
            auctionRepository.findById(event.auctionId()).ifPresent(auction -> {
                Map<String, Object> payload = new HashMap<>();
                Map<String, Object> data = new HashMap<>();

                // masking nama user menggunakan id untuk privasi sementara
                String maskedName = "User-" + event.newBidderId().toString().substring(0, 4) + "***";

                data.put("bidderName", maskedName);
                data.put("amount", event.newBidAmount());
                data.put("minimumNextBid", auction.getCurrentPrice().add(auction.getMinimumIncrement()));
                data.put("bidCount", auction.getBidCount());

                data.put("timestamp", java.time.LocalDateTime.now().toString());

                payload.put("type", "NEW_BID");
                payload.put("data", data);

                messagingTemplate.convertAndSend(AUCTION_TOPIC_PREFIX + event.auctionId(), payload);

                // cek dan broadcast juga jika terjadi perpanjangan waktu lelang (anti-sniping)
                if (auction.getExtensionCount() > 0) {
                    Map<String, Object> extPayload = new HashMap<>();
                    Map<String, Object> extData = new HashMap<>();
                    extData.put("newEndTime", auction.getEndTime().toString());
                    extData.put("extensionCount", auction.getExtensionCount());

                    extPayload.put("type", "AUCTION_EXTENDED");
                    extPayload.put("data", extData);

                    messagingTemplate.convertAndSend(AUCTION_TOPIC_PREFIX + event.auctionId(), extPayload);
                }
            });
        } catch (Exception e) {
            log.error("gagal mengirim pesan websocket untuk lelang: {}", event.auctionId(), e);
        }
    }

    @Async
    @EventListener
    public void handleWinnerDeterminedEvent(WinnerDeterminedEvent event) {
        // broadcast lelang selesai
        try {
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> data = new HashMap<>();

            String maskedName = "User-" + event.winnerId().toString().substring(0, 4) + "***";
            data.put("status", "WON");
            data.put("winningBid", event.winningAmount());
            data.put("winnerName", maskedName);

            payload.put("type", "AUCTION_ENDED");
            payload.put("data", data);

            messagingTemplate.convertAndSend(AUCTION_TOPIC_PREFIX + event.auctionId(), payload);
        } catch (Exception e) {
            log.error("gagal memproses websocket kemenangan lelang: {}", event.auctionId(), e);
        }
    }

    @Async
    @EventListener
    public void handleAuctionUnsoldEvent(AuctionUnsoldEvent event) {
        // broadcast lelang gagal terjual
        try {
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("status", "UNSOLD");

            payload.put("type", "AUCTION_ENDED");
            payload.put("data", data);

            messagingTemplate.convertAndSend(AUCTION_TOPIC_PREFIX + event.auctionId(), payload);
        } catch (Exception e) {
            log.error("gagal memproses websocket kegagalan lelang: {}", event.auctionId(), e);
        }
    }
}
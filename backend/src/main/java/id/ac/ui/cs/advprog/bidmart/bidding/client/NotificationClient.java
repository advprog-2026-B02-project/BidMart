package id.ac.ui.cs.advprog.bidmart.bidding.client;

import java.util.UUID;

public interface NotificationClient {
    void sendOutbidNotification(UUID userId, UUID auctionId);
    void sendAuctionWonNotification(UUID userId, UUID auctionId);
}
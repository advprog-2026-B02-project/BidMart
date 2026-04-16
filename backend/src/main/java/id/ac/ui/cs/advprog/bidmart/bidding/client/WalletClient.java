package id.ac.ui.cs.advprog.bidmart.bidding.client;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletClient {
    UUID holdFunds(UUID userId, UUID auctionId, BigDecimal amount);
    void releaseFunds(UUID holdId);
    void captureWinnerFunds(UUID auctionId, UUID winnerId);
    void releaseAllAuctionHolds(UUID auctionId);
}
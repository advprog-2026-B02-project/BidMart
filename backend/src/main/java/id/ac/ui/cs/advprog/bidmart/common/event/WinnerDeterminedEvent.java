package id.ac.ui.cs.advprog.bidmart.common.event;

import java.util.UUID;

public class WinnerDeterminedEvent {

    private final UUID auctionId;
    private final UUID winnerId;
    private final long winningAmount;

    public WinnerDeterminedEvent(UUID auctionId, UUID winnerId, long winningAmount) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.winningAmount = winningAmount;
    }

    public UUID getAuctionId() { return auctionId; }
    public UUID getWinnerId() { return winnerId; }
    public long getWinningAmount() { return winningAmount; }
}

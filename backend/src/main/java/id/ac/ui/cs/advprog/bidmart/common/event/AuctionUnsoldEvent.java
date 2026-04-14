package id.ac.ui.cs.advprog.bidmart.common.event;

import java.util.UUID;

public class AuctionUnsoldEvent {

    private final UUID auctionId;

    public AuctionUnsoldEvent(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getAuctionId() { return auctionId; }
}

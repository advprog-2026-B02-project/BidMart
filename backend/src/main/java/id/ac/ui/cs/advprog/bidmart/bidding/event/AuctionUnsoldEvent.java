package id.ac.ui.cs.advprog.bidmart.bidding.event;

import java.util.UUID;

public record AuctionUnsoldEvent(
        UUID auctionId
) {}
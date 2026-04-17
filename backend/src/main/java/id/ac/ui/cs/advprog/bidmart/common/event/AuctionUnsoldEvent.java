package id.ac.ui.cs.advprog.bidmart.common.event;

import java.util.UUID;

public record AuctionUnsoldEvent(
    UUID auctionId,
    UUID sellerId
) {}

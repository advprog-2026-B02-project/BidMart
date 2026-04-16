package id.ac.ui.cs.advprog.bidmart.bidding.event;

import java.math.BigDecimal;
import java.util.UUID;

public record AuctionWonEvent(
        UUID auctionId,
        UUID winnerId,
        BigDecimal winningAmount
) {}
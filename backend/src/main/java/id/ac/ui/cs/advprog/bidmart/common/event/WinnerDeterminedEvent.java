package id.ac.ui.cs.advprog.bidmart.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public record WinnerDeterminedEvent(
        UUID auctionId,
        UUID winnerId,
        BigDecimal winningAmount
) {}

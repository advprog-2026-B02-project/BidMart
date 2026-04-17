package id.ac.ui.cs.advprog.bidmart.common.event;

import java.math.BigDecimal;
import java.util.UUID;

// payload event saat bid berhasil, membawa data esensial untuk modul lain
public record BidPlacedEvent(
        UUID auctionId,
        UUID sellerId,
        UUID newBidderId,
        BigDecimal newBidAmount,
        UUID outbidUserId,    // bisa null jika ini bid pertama
        UUID outbidHoldId     // bisa null jika ini bid pertama
) {}
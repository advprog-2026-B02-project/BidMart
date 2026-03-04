package id.ac.ui.cs.advprog.bidmart.wallet.dto;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldRequest {

    @Nonnull
    private UUID userId;

    @Nonnull
    private UUID auctionId;

    private UUID bidId;

    @Min(value = 1, message = "Hold amount must be positive")
    private long amount;
}
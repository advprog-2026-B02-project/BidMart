package id.ac.ui.cs.advprog.bidmart.wallet.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldRequest {

    private UUID userId;

    @NotNull
    private UUID auctionId;

    private UUID bidId;

    @Min(value = 1, message = "Hold amount must be positive")
    private long amount;
}

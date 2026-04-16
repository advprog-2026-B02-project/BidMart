package id.ac.ui.cs.advprog.bidmart.bidding.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionStartRequestDTO {
    @NotNull
    @Positive
    private BigDecimal startPrice;

    @NotNull
    @Positive
    private BigDecimal minimumIncrement;

    @NotNull
    private BigDecimal reservePrice;

    @NotNull
    @Future
    private LocalDateTime endTime;
}
package id.ac.ui.cs.advprog.bidmart.bidding.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuctionResultDTO {
    private UUID auctionId;
    private String status; // won atau unsold
    private UUID winnerId;
    private BigDecimal winningBid;
    private Boolean reserveMet;
    private LocalDateTime closedAt;
}
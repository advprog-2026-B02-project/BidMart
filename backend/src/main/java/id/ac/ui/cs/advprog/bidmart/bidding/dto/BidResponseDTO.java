package id.ac.ui.cs.advprog.bidmart.bidding.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BidResponseDTO {
    private UUID id;
    private UUID auctionId;
    private UUID bidderId;
    private BigDecimal amount;
    private String status;
    private UUID holdId;
    private BigDecimal previousHighBid;
    private boolean isNewHighBid;
    private LocalDateTime auctionEndTime;
    private LocalDateTime createdAt;
}
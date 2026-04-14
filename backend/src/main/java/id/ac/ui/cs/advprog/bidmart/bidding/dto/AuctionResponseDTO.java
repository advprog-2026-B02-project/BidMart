package id.ac.ui.cs.advprog.bidmart.bidding.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuctionResponseDTO {
    private UUID id;
    private UUID listingId;
    private String status;
    private BigDecimal currentPrice;
    private BigDecimal minimumNextBid;
    private UUID highestBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime;
    private Integer extensionCount;
    private Boolean reserveMet;
}
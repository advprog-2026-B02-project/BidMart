package id.ac.ui.cs.advprog.bidmart.bidding.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auctions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID listingId;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    private BigDecimal currentPrice;
    private BigDecimal minimumIncrement;
    private BigDecimal reservePrice;

    @Builder.Default
    private Integer bidCount = 0;

    private UUID highestBidderId;
    private String highestBidderName;
    private UUID highestBidderHoldId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime originalEndTime;

    @Builder.Default
    private Integer extensionCount = 0;

    @Builder.Default
    private Boolean reserveMet = false;

    // implementasi optimistic locking pada entitas
    @Version
    private Long version;
}
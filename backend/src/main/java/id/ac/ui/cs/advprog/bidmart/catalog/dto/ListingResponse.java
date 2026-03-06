package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ListingResponse {

    private UUID id;
    private UUID sellerId;
    private UUID categoryId;
    private String title;
    private String description;
    private ListingStatus status;
    private BigDecimal startingPrice;
    private BigDecimal reservePrice;
    private BigDecimal minimumIncrement;
    private Long auctionDuration;
    private BigDecimal currentPrice;
    private Integer bidCount;
    private Instant createdAt;
    private List<ListingImageResponse> images;

    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getSellerId(),
                listing.getCategoryId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getStatus(),
                listing.getStartingPrice(),
                listing.getReservePrice(),
                listing.getMinimumIncrement(),
                listing.getAuctionDuration(),
                listing.getCurrentPrice(),
                listing.getBidCount(),
                listing.getCreatedAt(),
                listing.getImages().stream()
                        .map(ListingImageResponse::from)
                        .toList()
        );
    }
}
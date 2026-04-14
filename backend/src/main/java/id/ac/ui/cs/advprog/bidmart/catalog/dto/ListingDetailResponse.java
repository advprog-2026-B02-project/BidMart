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
public class ListingDetailResponse {

    private UUID id;
    private UUID sellerId;
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    private String title;
    private String description;
    private ListingStatus status;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private BigDecimal minimumIncrement;
    private Integer bidCount;
    private Instant createdAt;
    private Instant activatedAt;
    private Instant auctionEndTime;
    private boolean auctionOngoing;
    private List<ListingImageResponse> images;

    public static ListingDetailResponse from(Listing listing) {
        String categoryName = listing.getCategory() != null
                ? listing.getCategory().getName()
                : null;
        String categorySlug = listing.getCategory() != null
                ? listing.getCategory().getSlug()
                : null;

        return new ListingDetailResponse(
                listing.getId(),
                listing.getSellerId(),
                listing.getCategoryId(),
                categoryName,
                categorySlug,
                listing.getTitle(),
                listing.getDescription(),
                listing.getStatus(),
                listing.getStartingPrice(),
                listing.getCurrentPrice(),
                listing.getMinimumIncrement(),
                listing.getBidCount(),
                listing.getCreatedAt(),
                listing.getActivatedAt(),
                listing.getAuctionEndTime(),
                listing.isAuctionOngoing(),
                listing.getImages().stream()
                        .map(ListingImageResponse::from)
                        .toList()
        );
    }
}
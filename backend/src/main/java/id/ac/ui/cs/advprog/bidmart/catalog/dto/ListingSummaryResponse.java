package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Getter
@AllArgsConstructor
public class ListingSummaryResponse {

    private UUID id;
    private String title;
    private UUID categoryId;
    private String categoryName;
    private ListingStatus status;
    private BigDecimal currentPrice;
    private BigDecimal startingPrice;
    private Integer bidCount;
    private Instant auctionEndTime;

    private String thumbnailUrl;

    public static ListingSummaryResponse from(Listing listing) {
        String categoryName = listing.getCategory() != null
                ? listing.getCategory().getName()
                : null;

        String thumbnail = listing.getImages().isEmpty()
                ? null
                : listing.getImages().get(0).getThumbnailUrl();

        return new ListingSummaryResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getCategoryId(),
                categoryName,
                listing.getStatus(),
                listing.getCurrentPrice(),
                listing.getStartingPrice(),
                listing.getBidCount(),
                listing.getAuctionEndTime(),
                thumbnail
        );
    }
}
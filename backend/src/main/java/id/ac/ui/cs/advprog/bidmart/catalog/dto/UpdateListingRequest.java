package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateListingRequest {

    private UUID categoryId;

    private String title;

    private String description;

    private BigDecimal startingPrice;

    private BigDecimal reservePrice;

    private BigDecimal minimumIncrement;

    private Long auctionDuration;

    private ListingStatus status;
}
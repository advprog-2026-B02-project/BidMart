package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateListingRequest {

        @NotNull
        private UUID categoryId;

        @NotBlank
        @Size(max = 200)
        private String title;

        private String description;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal startingPrice;

        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal reservePrice;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        private BigDecimal minimumIncrement;

        @NotNull
        @Min(60)
        private Long auctionDuration;

        private List<ListingImageRequest> images;
}
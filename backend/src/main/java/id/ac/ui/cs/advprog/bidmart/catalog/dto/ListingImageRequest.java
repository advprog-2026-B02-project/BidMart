package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingImageRequest {

        @NotBlank
        @Size(max = 2048)
        private String url;

        @Size(max = 2048)
        private String thumbnailUrl;

        @Min(0)
        private int displayOrder;
}
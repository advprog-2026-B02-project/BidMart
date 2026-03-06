package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddListingImageRequest {

    @NotBlank
    private String url;

    private String thumbnailUrl;

    @Min(0)
    private int displayOrder;
}
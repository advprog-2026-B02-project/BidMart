package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ListingImageResponse {

    private UUID id;
    private String url;
    private String thumbnailUrl;
    private int displayOrder;

    public static ListingImageResponse from(ListingImage image) {
        return new ListingImageResponse(
                image.getId(),
                image.getUrl(),
                image.getThumbnailUrl(),
                image.getDisplayOrder()
        );
    }
}
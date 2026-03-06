package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.AddListingImageRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingImageResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingImage;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.ListingImageRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.ListingRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/listings/{listingId}/images")
@RequiredArgsConstructor
public class ListingImageController {

    private final ListingRepository      listingRepository;
    private final ListingImageRepository listingImageRepository;


    @PostMapping
    public ResponseEntity<ListingImageResponse> addImage(
            @PathVariable UUID listingId,
            @RequestHeader("X-Seller-Id") UUID sellerId,
            @Valid @RequestBody AddListingImageRequest request
    ) {
        Listing listing = getListingOrThrow(listingId);
        assertOwner(listing, sellerId);

        ListingImage image = ListingImage.builder()
                .listingId(listingId)
                .url(request.getUrl())
                .thumbnailUrl(request.getThumbnailUrl())
                .displayOrder(request.getDisplayOrder())
                .build();

        listing.getImages().add(image);
        listingRepository.save(listing);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ListingImageResponse.from(listing.getImages().getLast()));
    }


    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> removeImage(
            @PathVariable UUID listingId,
            @PathVariable UUID imageId,
            @RequestHeader("X-Seller-Id") UUID sellerId
    ) {
        Listing listing = getListingOrThrow(listingId);
        assertOwner(listing, sellerId);

        ListingImage image = listingImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Gambar tidak ditemukan: " + imageId));

        listing.getImages().remove(image);
        listingRepository.save(listing);

        return ResponseEntity.noContent().build();
    }


    private Listing getListingOrThrow(UUID listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Listing tidak ditemukan: " + listingId));
    }

    private void assertOwner(Listing listing, UUID sellerId) {
        if (!listing.getSellerId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda bukan pemilik listing ini.");
        }
    }
}
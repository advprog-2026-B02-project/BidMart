package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingImageRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingImageResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingImage;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.ListingImageRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.ListingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingImageControllerTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private ListingImageRepository listingImageRepository;

    @InjectMocks
    private ListingImageController controller;

    @Test
    void addImage() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        ListingImageRequest req = new ListingImageRequest();
        req.setUrl("url");

        Listing listing = new Listing();
        listing.setSellerId(sellerId);
        listing.setImages(new ArrayList<>());
        
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        ResponseEntity<ListingImageResponse> res = controller.addImage(listingId, sellerId, req);
        assertEquals(201, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void removeImage() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();

        Listing listing = new Listing();
        listing.setSellerId(sellerId);
        listing.setImages(new ArrayList<>());
        ListingImage img = new ListingImage();
        listing.getImages().add(img);

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingImageRepository.findById(imageId)).thenReturn(Optional.of(img));

        ResponseEntity<Void> res = controller.removeImage(listingId, imageId, sellerId);
        assertEquals(204, res.getStatusCode().value());
    }
}

package id.ac.ui.cs.advprog.bidmart.catalog.dto;

import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogDtoTest {

    @Test
    void testListingDetailResponse() {
        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setTitle("Title");
        listing.setDescription("desc");
        ListingDetailResponse res = ListingDetailResponse.from(listing);
        assertEquals("desc", res.getDescription());
        assertEquals("Title", res.getTitle());
    }

    @Test
    void testListingSummaryResponse() {
        Listing listing = new Listing();
        listing.setId(UUID.randomUUID());
        listing.setTitle("Title");
        ListingSummaryResponse res = ListingSummaryResponse.from(listing);
        assertEquals("Title", res.getTitle());
    }

    @Test
    void testCreateListingRequest() {
        CreateListingRequest req = new CreateListingRequest();
        req.setCategoryId(UUID.randomUUID());
        req.setTitle("T");
        req.setDescription("D");
        req.setStartingPrice(BigDecimal.ONE);
        req.setReservePrice(BigDecimal.ONE);
        req.setMinimumIncrement(BigDecimal.ONE);
        req.setAuctionDuration(1L);

        assertEquals("T", req.getTitle());
        assertEquals("D", req.getDescription());
    }

    @Test
    void testUpdateListingRequest() {
        UpdateListingRequest req = new UpdateListingRequest();
        req.setCategoryId(UUID.randomUUID());
        req.setTitle("T");
        req.setDescription("D");
        req.setStartingPrice(BigDecimal.ONE);
        req.setReservePrice(BigDecimal.ONE);
        req.setMinimumIncrement(BigDecimal.ONE);
        req.setAuctionDuration(1L);

        assertEquals("T", req.getTitle());
    }
}

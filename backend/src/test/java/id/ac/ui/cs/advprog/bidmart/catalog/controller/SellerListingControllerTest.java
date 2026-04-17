package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.UpdateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerListingControllerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private SellerListingController controller;

    @Test
    void myListings() {
        UUID sellerId = UUID.randomUUID();
        when(listingService.findBySeller(sellerId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<ListingResponse>> res = controller.myListings(sellerId);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void create() {
        UUID sellerId = UUID.randomUUID();
        CreateListingRequest req = new CreateListingRequest();
        ListingResponse listed = mock(ListingResponse.class);

        when(listingService.create(sellerId, req)).thenReturn(listed);

        ResponseEntity<ListingResponse> res = controller.create(sellerId, req);
        assertEquals(201, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void edit() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UpdateListingRequest req = new UpdateListingRequest();
        ListingResponse updated = mock(ListingResponse.class);

        when(listingService.update(listingId, sellerId, req)).thenReturn(updated);

        ResponseEntity<ListingResponse> res = controller.edit(listingId, sellerId, req);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void activate() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        ListingResponse activated = mock(ListingResponse.class);

        when(listingService.activate(listingId, sellerId)).thenReturn(activated);

        ResponseEntity<ListingResponse> res = controller.activate(listingId, sellerId);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void cancel() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        doNothing().when(listingService).cancel(listingId, sellerId);

        ResponseEntity<Void> res = controller.cancel(listingId, sellerId);
        assertEquals(204, res.getStatusCode().value());
        verify(listingService).cancel(listingId, sellerId);
    }

    @Test
    void delete() {
        UUID listingId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        doNothing().when(listingService).delete(listingId, sellerId);

        ResponseEntity<Void> res = controller.delete(listingId, sellerId);
        assertEquals(204, res.getStatusCode().value());
        verify(listingService).delete(listingId, sellerId);
    }
}

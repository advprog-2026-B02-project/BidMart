package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.controller.BidSyncController.SyncPriceRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BidSyncControllerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private BidSyncController controller;

    @Test
    void validateForBid() {
        UUID id = UUID.randomUUID();
        doNothing().when(listingService).validateListingForBid(id);
        
        ResponseEntity<Void> res = controller.validateForBid(id);
        assertEquals(200, res.getStatusCode().value());
        verify(listingService).validateListingForBid(id);
    }

    @Test
    void syncPrice() {
        UUID id = UUID.randomUUID();
        SyncPriceRequest req = new SyncPriceRequest(BigDecimal.TEN, 2);
        
        doNothing().when(listingService).syncPrice(id, req.getNewPrice(), req.getBidCount());

        ResponseEntity<Void> res = controller.syncPrice(id, req);
        assertEquals(200, res.getStatusCode().value());
        verify(listingService).syncPrice(id, req.getNewPrice(), req.getBidCount());
    }
}

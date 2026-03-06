package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;


@RestController
@RequestMapping("/internal/listings")
@RequiredArgsConstructor
public class BidSyncController {

    private final ListingService listingService;

    @PostMapping("/{listingId}/validate-bid")
    public ResponseEntity<Void> validateForBid(@PathVariable UUID listingId) {
        listingService.validateListingForBid(listingId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{listingId}/sync-price")
    public ResponseEntity<Void> syncPrice(
            @PathVariable UUID listingId,
            @RequestBody SyncPriceRequest request
    ) {
        listingService.syncPrice(listingId, request.getNewPrice(), request.getBidCount());
        return ResponseEntity.ok().build();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyncPriceRequest {
        private BigDecimal newPrice;
        private int bidCount;
    }
}
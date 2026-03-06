package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.UpdateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/seller/listings")
@RequiredArgsConstructor
public class SellerListingController {

    private final ListingService listingService;


    @GetMapping
    public ResponseEntity<List<ListingResponse>> myListings(
            @RequestHeader("X-Seller-Id") UUID sellerId
    ) {
        return ResponseEntity.ok(listingService.findBySeller(sellerId));
    }

    @PostMapping("/new")
    public ResponseEntity<ListingResponse> create(
            @RequestHeader("X-Seller-Id") UUID sellerId,
            @Valid @RequestBody CreateListingRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listingService.create(sellerId, request));
    }

    @PutMapping("/{listingId}/edit")
    public ResponseEntity<ListingResponse> edit(
            @PathVariable UUID listingId,
            @RequestHeader("X-Seller-Id") UUID sellerId,
            @Valid @RequestBody UpdateListingRequest request
    ) {
        return ResponseEntity.ok(listingService.update(listingId, sellerId, request));
    }

    @PatchMapping("/{listingId}/activate")
    public ResponseEntity<ListingResponse> activate(
            @PathVariable UUID listingId,
            @RequestHeader("X-Seller-Id") UUID sellerId
    ) {
        return ResponseEntity.ok(listingService.activate(listingId, sellerId));
    }

    @PatchMapping("/{listingId}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID listingId,
            @RequestHeader("X-Seller-Id") UUID sellerId
    ) {
        listingService.cancel(listingId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID listingId,
            @RequestHeader("X-Seller-Id") UUID sellerId
    ) {
        listingService.delete(listingId, sellerId);
        return ResponseEntity.noContent().build();
    }
}
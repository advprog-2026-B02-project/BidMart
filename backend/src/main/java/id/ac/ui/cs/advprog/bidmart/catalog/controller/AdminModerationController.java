package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.ModerationRequest.ModerateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;


@RestController
@RequestMapping("/admin/moderation/listings")
@RequiredArgsConstructor
public class AdminModerationController {

    private final ListingService listingService;

    @GetMapping
    public ResponseEntity<Page<ListingResponse>> findAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(listingService.findAllForAdmin(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ListingDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(listingService.findDetailById(id));
    }


    @PatchMapping("/{id}")
    public ResponseEntity<ListingResponse> moderate(
            @PathVariable UUID id,
            @Valid @RequestBody ModerateListingRequest request
    ) {
        return ResponseEntity.ok(listingService.moderate(id, request));
    }
}
package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingSummaryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@RestController
@RequiredArgsConstructor
public class CatalogController {

    private final ListingService listingService;


    @GetMapping("/catalog")
    public ResponseEntity<Page<ListingSummaryResponse>> browse(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Instant endsBefore,
            @PageableDefault(size = 20, sort = "currentPrice") Pageable pageable
    ) {
        return ResponseEntity.ok(
                listingService.findForCatalog(q, category, minPrice, maxPrice, endsBefore, pageable)
        );
    }


    @GetMapping("/listings/{listingId}")
    public ResponseEntity<ListingDetailResponse> detail(@PathVariable UUID listingId) {
        return ResponseEntity.ok(listingService.findDetailById(listingId));
    }
}
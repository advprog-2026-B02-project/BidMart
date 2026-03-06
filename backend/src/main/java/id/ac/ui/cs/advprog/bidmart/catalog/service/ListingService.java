package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ModerationRequest.ModerateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.UpdateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingSummaryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ListingService {


    ListingResponse create(UUID sellerId, CreateListingRequest request);

    ListingDetailResponse findDetailById(UUID id);

    List<ListingResponse> findBySeller(UUID sellerId);

    ListingResponse update(UUID id, UUID sellerId, UpdateListingRequest request);


    ListingResponse activate(UUID id, UUID sellerId);


    void cancel(UUID id, UUID sellerId);

    void delete(UUID id, UUID sellerId);


    Page<ListingSummaryResponse> findForCatalog(
            String keyword,
            UUID categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Instant endsBefore,
            Pageable pageable
    );


    Page<ListingResponse> findAllForAdmin(Pageable pageable);

    ListingResponse moderate(UUID id, ModerateListingRequest request);

    void validateListingForBid(UUID listingId);

    void syncPrice(UUID listingId, BigDecimal newPrice, int bidCount);
}
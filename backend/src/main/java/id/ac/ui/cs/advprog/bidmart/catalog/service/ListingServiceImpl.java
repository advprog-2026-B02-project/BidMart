package id.ac.ui.cs.advprog.bidmart.catalog.service.impl;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.request.CreateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.request.ListingImageRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.request.ModerationRequest.Action;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.request.ModerationRequest.ModerateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.request.UpdateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.response.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.response.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.response.ListingSummaryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.exception.ForbiddenException;
import id.ac.ui.cs.advprog.bidmart.catalog.exception.InvalidStatusTransitionException;
import id.ac.ui.cs.advprog.bidmart.catalog.exception.ResourceNotFoundException;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingImage;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public ListingResponse create(UUID sellerId, CreateListingRequest request) {
        validateCategoryExists(request.getCategoryId());

        Listing listing = Listing.builder()
                .sellerId(sellerId)
                .categoryId(request.getCategoryId())
                .title(request.getTitle())
                .description(request.getDescription())
                .startingPrice(request.getStartingPrice())
                .currentPrice(request.getStartingPrice())
                .reservePrice(request.getReservePrice())
                .minimumIncrement(request.getMinimumIncrement())
                .auctionDuration(request.getAuctionDuration())
                .status(ListingStatus.DRAFT)
                .build();

        if (request.getImages() != null) {
            request.getImages().forEach(img ->
                    listing.getImages().add(buildImage(img, listing)));
        }

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Override
    @Transactional(readOnly = true)
    public ListingDetailResponse findDetailById(UUID id) {
        return ListingDetailResponse.from(getOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ListingResponse> findBySeller(UUID sellerId) {
        return listingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream().map(ListingResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ListingSummaryResponse> findForCatalog(
            String keyword,
            UUID categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Instant endsBefore,
            Pageable pageable
    ) {
        // Hanya tampilkan listing ACTIVE di katalog pembeli
        return listingRepository
                .findByFilters(keyword, categoryId, ListingStatus.ACTIVE,
                        minPrice, maxPrice, endsBefore, pageable)
                .map(ListingSummaryResponse::from);
    }

    @Override
    @Transactional
    public ListingResponse update(UUID id, UUID sellerId, UpdateListingRequest request) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getStatus() == ListingStatus.CLOSED) {
            throw new IllegalStateException("Listing yang sudah CLOSED tidak dapat diubah.");
        }
        if (listing.getStatus() == ListingStatus.ACTIVE && listing.getBidCount() > 0) {
            throw new IllegalStateException(
                    "Listing tidak dapat diubah setelah ada penawaran masuk.");
        }

        if (request.getCategoryId()     != null) {
            validateCategoryExists(request.getCategoryId());
            listing.setCategoryId(request.getCategoryId());
        }
        if (request.getTitle()          != null) listing.setTitle(request.getTitle());
        if (request.getDescription()    != null) listing.setDescription(request.getDescription());
        if (request.getStartingPrice()  != null) listing.setStartingPrice(request.getStartingPrice());
        if (request.getReservePrice()   != null) listing.setReservePrice(request.getReservePrice());
        if (request.getMinimumIncrement() != null) listing.setMinimumIncrement(request.getMinimumIncrement());
        if (request.getAuctionDuration()  != null) listing.setAuctionDuration(request.getAuctionDuration());

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Override
    @Transactional
    public ListingResponse activate(UUID id, UUID sellerId) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new InvalidStatusTransitionException(listing.getStatus(), ListingStatus.ACTIVE);
        }

        listing.setStatus(ListingStatus.ACTIVE);
        listing.setActivatedAt(Instant.now());        // catat waktu mulai lelang
        listing.setCurrentPrice(listing.getStartingPrice());

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Override
    @Transactional
    public void cancel(UUID id, UUID sellerId) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getBidCount() > 0) {
            throw new IllegalStateException(
                    "Listing tidak dapat dibatalkan setelah ada penawaran masuk.");
        }
        if (listing.getStatus() == ListingStatus.CLOSED) {
            throw new IllegalStateException("Listing sudah berstatus CLOSED.");
        }

        listing.setStatus(ListingStatus.CLOSED);
        listingRepository.save(listing);
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID sellerId) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new IllegalStateException("Hanya listing DRAFT yang dapat dihapus.");
        }

        listingRepository.delete(listing);
    }



    @Override
    @Transactional(readOnly = true)
    public Page<ListingResponse> findAllForAdmin(Pageable pageable) {
        return listingRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ListingResponse::from);
    }

    @Override
    @Transactional
    public ListingResponse moderate(UUID id, ModerateListingRequest request) {
        Listing listing = getOrThrow(id);

        switch (request.getAction()) {
            case APPROVE -> {
                if (listing.getStatus() != ListingStatus.DRAFT) {
                    throw new IllegalStateException("Hanya listing DRAFT yang dapat disetujui.");
                }
                listing.setStatus(ListingStatus.ACTIVE);
                listing.setActivatedAt(Instant.now());
                listing.setCurrentPrice(listing.getStartingPrice());
            }
            case REJECT, DELETE -> {
                if (listing.getStatus() == ListingStatus.CLOSED) {
                    throw new IllegalStateException("Listing sudah berstatus CLOSED.");
                }
                listing.setStatus(ListingStatus.CLOSED);
            }
        }

        return ListingResponse.from(listingRepository.save(listing));
    }

    @Override
    @Transactional(readOnly = true)
    public void validateListingForBid(UUID listingId) {
        Listing listing = getOrThrow(listingId);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Listing tidak aktif. Status saat ini: " + listing.getStatus());
        }

        if (!listing.isAuctionOngoing()) {
            throw new IllegalStateException(
                    "Waktu lelang sudah berakhir pada: " + listing.getAuctionEndTime());
        }
    }

    @Override
    @Transactional
    public void syncPrice(UUID listingId, BigDecimal newPrice, int bidCount) {
        int updated = listingRepository.syncPrice(listingId, newPrice, bidCount);
        if (updated == 0) {
            throw new ResourceNotFoundException("Listing tidak ditemukan: " + listingId);
        }
    }



    private Listing getOrThrow(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Listing tidak ditemukan: " + id));
    }

    private void assertOwner(Listing listing, UUID sellerId) {
        if (!listing.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("Anda bukan pemilik listing ini.");
        }
    }

    private void validateCategoryExists(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category tidak ditemukan: " + categoryId);
        }
    }

    private ListingImage buildImage(ListingImageRequest req, Listing listing) {
        return ListingImage.builder()
                .listing(listing)
                .url(req.getUrl())
                .thumbnailUrl(req.getThumbnailUrl())
                .displayOrder(req.getDisplayOrder())
                .build();
    }
}
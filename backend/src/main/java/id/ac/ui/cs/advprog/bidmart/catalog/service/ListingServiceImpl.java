package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingImageRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ModerationRequest.ModerateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.UpdateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingSummaryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingImage;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.CategoryRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.ListingRepository;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListingServiceImpl implements ListingService {

    private final ListingRepository  listingRepository;
    private final CategoryRepository categoryRepository;

    // ── Create ────────────────────────────────────────────────────────────────

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

    // ── Read ──────────────────────────────────────────────────────────────────

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
        return listingRepository
                .findByFilters(keyword, categoryId, ListingStatus.ACTIVE,
                        minPrice, maxPrice, endsBefore, pageable)
                .map(ListingSummaryResponse::from);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ListingResponse update(UUID id, UUID sellerId, UpdateListingRequest request) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getStatus() == ListingStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Listing yang sudah CLOSED tidak dapat diubah.");
        }
        if (listing.getStatus() == ListingStatus.ACTIVE && listing.getBidCount() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Listing tidak dapat diubah setelah ada penawaran masuk.");
        }

        if (request.getCategoryId()       != null) {
            validateCategoryExists(request.getCategoryId());
            listing.setCategoryId(request.getCategoryId());
        }
        if (request.getTitle()            != null) listing.setTitle(request.getTitle());
        if (request.getDescription()      != null) listing.setDescription(request.getDescription());
        if (request.getStartingPrice()    != null) listing.setStartingPrice(request.getStartingPrice());
        if (request.getReservePrice()     != null) listing.setReservePrice(request.getReservePrice());
        if (request.getMinimumIncrement() != null) listing.setMinimumIncrement(request.getMinimumIncrement());
        if (request.getAuctionDuration()  != null) listing.setAuctionDuration(request.getAuctionDuration());

        return ListingResponse.from(listingRepository.save(listing));
    }

    // ── Activate (DRAFT → ACTIVE) ─────────────────────────────────────────────

    @Override
    @Transactional
    public ListingResponse activate(UUID id, UUID sellerId) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transisi status tidak valid: " + listing.getStatus() + " → ACTIVE");
        }

        listing.setStatus(ListingStatus.ACTIVE);
        listing.setActivatedAt(Instant.now());
        listing.setCurrentPrice(listing.getStartingPrice());

        return ListingResponse.from(listingRepository.save(listing));
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void cancel(UUID id, UUID sellerId) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getBidCount() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Listing tidak dapat dibatalkan setelah ada penawaran masuk.");
        }
        if (listing.getStatus() == ListingStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Listing sudah berstatus CLOSED.");
        }

        listing.setStatus(ListingStatus.CLOSED);
        listingRepository.save(listing);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void delete(UUID id, UUID sellerId) {
        Listing listing = getOrThrow(id);
        assertOwner(listing, sellerId);

        if (listing.getStatus() != ListingStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Hanya listing DRAFT yang dapat dihapus.");
        }

        listingRepository.delete(listing);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

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
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Hanya listing DRAFT yang dapat disetujui.");
                }
                listing.setStatus(ListingStatus.ACTIVE);
                listing.setActivatedAt(Instant.now());
                listing.setCurrentPrice(listing.getStartingPrice());
            }
            case REJECT, DELETE -> {
                if (listing.getStatus() == ListingStatus.CLOSED) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Listing sudah berstatus CLOSED.");
                }
                listing.setStatus(ListingStatus.CLOSED);
            }
        }

        return ListingResponse.from(listingRepository.save(listing));
    }

    // ── [LOGIC] Validasi listing untuk penawaran ──────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public void validateListingForBid(UUID listingId) {
        Listing listing = getOrThrow(listingId);

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Listing tidak aktif. Status saat ini: " + listing.getStatus());
        }
        if (!listing.isAuctionOngoing()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Waktu lelang sudah berakhir pada: " + listing.getAuctionEndTime());
        }
    }

    // ── [LOGIC] Sinkronisasi harga dari bid service ───────────────────────────

    @Override
    @Transactional
    public void syncPrice(UUID listingId, BigDecimal newPrice, int bidCount) {
        int updated = listingRepository.syncPrice(listingId, newPrice, bidCount);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Listing tidak ditemukan: " + listingId);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Listing getOrThrow(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Listing tidak ditemukan: " + id));
    }

    private void assertOwner(Listing listing, UUID sellerId) {
        if (!listing.getSellerId().equals(sellerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda bukan pemilik listing ini.");
        }
    }

    private void validateCategoryExists(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Category tidak ditemukan: " + categoryId);
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
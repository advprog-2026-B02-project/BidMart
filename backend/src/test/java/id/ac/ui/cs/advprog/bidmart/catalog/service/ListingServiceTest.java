package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.CreateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingImageRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ModerationRequest.Action;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ListingServiceImpl listingService;

    private Listing listing;
    private UUID listingId;
    private UUID sellerId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        listingId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        listing = Listing.builder()
                .id(listingId)
                .sellerId(sellerId)
                .categoryId(categoryId)
                .title("Test Listing")
                .description("Test Description")
                .status(ListingStatus.DRAFT)
                .startingPrice(new BigDecimal("100.00"))
                .reservePrice(new BigDecimal("150.00"))
                .minimumIncrement(new BigDecimal("10.00"))
                .auctionDuration(3600L)
                .currentPrice(new BigDecimal("100.00"))
                .bidCount(0)
                .images(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testCreate_SuccessWithoutImages() {
        CreateListingRequest req = CreateListingRequest.builder()
                .categoryId(categoryId)
                .title("Title")
                .startingPrice(BigDecimal.TEN)
                .reservePrice(BigDecimal.TEN)
                .minimumIncrement(BigDecimal.ONE)
                .auctionDuration(3600L)
                .build();

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(listingRepository.save(any(Listing.class))).thenAnswer(i -> {
            Listing saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ListingResponse response = listingService.create(sellerId, req);
        assertNotNull(response);
        verify(categoryRepository).existsById(categoryId);
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    void testCreate_SuccessWithImages() {
        ListingImageRequest imgReq = new ListingImageRequest();
        imgReq.setUrl("img.png");
        imgReq.setThumbnailUrl("thmb.png");
        imgReq.setDisplayOrder(1);

        CreateListingRequest req = CreateListingRequest.builder()
                .categoryId(categoryId)
                .title("Title")
                .startingPrice(BigDecimal.TEN)
                .minimumIncrement(BigDecimal.ONE)
                .auctionDuration(3600L)
                .images(List.of(imgReq))
                .build();

        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(listingRepository.save(any(Listing.class))).thenAnswer(i -> {
            Listing saved = i.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        ListingResponse response = listingService.create(sellerId, req);
        assertNotNull(response);
    }

    @Test
    void testCreate_CategoryNotFound() {
        CreateListingRequest req = CreateListingRequest.builder().categoryId(categoryId).build();
        when(categoryRepository.existsById(categoryId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> listingService.create(sellerId, req));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testFindDetailById_Success() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        ListingDetailResponse response = listingService.findDetailById(listingId);
        assertNotNull(response);
    }

    @Test
    void testFindDetailById_NotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> listingService.findDetailById(listingId));
    }

    @Test
    void testFindBySeller() {
        when(listingRepository.findBySellerIdOrderByCreatedAtDesc(sellerId))
                .thenReturn(List.of(listing));

        List<ListingResponse> res = listingService.findBySeller(sellerId);
        assertEquals(1, res.size());
    }

    @Test
    void testFindForCatalog() {
        Page<Listing> page = new PageImpl<>(List.of(listing));
        Pageable pageable = PageRequest.of(0, 10);
        when(listingRepository.findByFilters(eq("kw"), eq(categoryId), eq(ListingStatus.ACTIVE), any(), any(), any(), eq(pageable)))
                .thenReturn(page);

        Page<ListingSummaryResponse> res = listingService.findForCatalog("kw", categoryId, null, null, null, pageable);
        assertEquals(1, res.getTotalElements());
    }

    @Test
    void testUpdate_Success() {
        UpdateListingRequest req = UpdateListingRequest.builder()
                .categoryId(categoryId)
                .title("New")
                .description("Desc")
                .startingPrice(BigDecimal.TEN)
                .reservePrice(BigDecimal.TEN)
                .minimumIncrement(BigDecimal.ONE)
                .auctionDuration(3600L)
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(categoryRepository.existsById(categoryId)).thenReturn(true);
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        ListingResponse res = listingService.update(listingId, sellerId, req);
        assertNotNull(res);
        assertEquals("New", listing.getTitle());
    }

    @Test
    void testUpdate_PartialFields() {
        UpdateListingRequest req = UpdateListingRequest.builder()
                .title("New Title Only")
                .build();

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        listingService.update(listingId, sellerId, req);
        assertEquals("New Title Only", listing.getTitle());
        assertEquals(categoryId, listing.getCategoryId()); // Unchanged
    }

    @Test
    void testUpdate_NullFields() {
        UpdateListingRequest req = new UpdateListingRequest(); // All null
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        listingService.update(listingId, sellerId, req);
        assertNotNull(listing.getTitle()); // Should not have changed to null
    }

    @Test
    void testUpdate_ActiveWithoutBids() {
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setBidCount(0);
        UpdateListingRequest req = UpdateListingRequest.builder().title("New").build();
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        assertDoesNotThrow(() -> listingService.update(listingId, sellerId, req));
    }

    @Test
    void testUpdate_NotOwner() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        assertThrows(ResponseStatusException.class, () -> listingService.update(listingId, UUID.randomUUID(), new UpdateListingRequest()));
    }

    @Test
    void testUpdate_ClosedListing() {
        listing.setStatus(ListingStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        assertThrows(ResponseStatusException.class, () -> listingService.update(listingId, sellerId, new UpdateListingRequest()));
    }

    @Test
    void testUpdate_ActiveWithBids() {
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setBidCount(1);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        assertThrows(ResponseStatusException.class, () -> listingService.update(listingId, sellerId, new UpdateListingRequest()));
    }

    @Test
    void testActivate_Success() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        listingService.activate(listingId, sellerId);
        assertEquals(ListingStatus.ACTIVE, listing.getStatus());
    }

    @Test
    void testActivate_NotDraft() {
        listing.setStatus(ListingStatus.ACTIVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        assertThrows(ResponseStatusException.class, () -> listingService.activate(listingId, sellerId));
    }

    @Test
    void testCancel_Success() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        listingService.cancel(listingId, sellerId);
        assertEquals(ListingStatus.CLOSED, listing.getStatus());
        verify(listingRepository).save(listing);
    }

    @Test
    void testCancel_WithBids() {
        listing.setBidCount(1);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        assertThrows(ResponseStatusException.class, () -> listingService.cancel(listingId, sellerId));
    }

    @Test
    void testCancel_AlreadyClosed() {
        listing.setStatus(ListingStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        assertThrows(ResponseStatusException.class, () -> listingService.cancel(listingId, sellerId));
    }

    @Test
    void testDelete_Success() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        listingService.delete(listingId, sellerId);
        verify(listingRepository).delete(listing);
    }

    @Test
    void testDelete_NotDraft() {
        listing.setStatus(ListingStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        assertThrows(ResponseStatusException.class, () -> listingService.delete(listingId, sellerId));
    }

    @Test
    void testFindAllForAdmin() {
        Page<Listing> page = new PageImpl<>(List.of(listing));
        when(listingRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);
        Page<ListingResponse> res = listingService.findAllForAdmin(PageRequest.of(0, 10));
        assertEquals(1, res.getTotalElements());
    }

    @Test
    void testModerate_Approve() {
        ModerateListingRequest req = new ModerateListingRequest();
        req.setAction(Action.APPROVE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenReturn(listing);

        listingService.moderate(listingId, req);
        assertEquals(ListingStatus.ACTIVE, listing.getStatus());
    }

    @Test
    void testModerate_ApproveNotDraft() {
        ModerateListingRequest req = new ModerateListingRequest();
        req.setAction(Action.APPROVE);
        listing.setStatus(ListingStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(ResponseStatusException.class, () -> listingService.moderate(listingId, req));
    }

    @Test
    void testModerate_Reject() {
        ModerateListingRequest req = new ModerateListingRequest();
        req.setAction(Action.REJECT);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenReturn(listing);

        listingService.moderate(listingId, req);
        assertEquals(ListingStatus.CLOSED, listing.getStatus());
    }

    @Test
    void testModerate_Delete() {
        ModerateListingRequest req = new ModerateListingRequest();
        req.setAction(Action.DELETE);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenReturn(listing);

        listingService.moderate(listingId, req);
        assertEquals(ListingStatus.CLOSED, listing.getStatus());
    }

    @Test
    void testModerate_RejectAlreadyClosed() {
        ModerateListingRequest req = new ModerateListingRequest();
        req.setAction(Action.REJECT);
        listing.setStatus(ListingStatus.CLOSED);
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(ResponseStatusException.class, () -> listingService.moderate(listingId, req));
    }

    @Test
    void testValidateListingForBid() {
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setActivatedAt(Instant.now().minusSeconds(10));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        
        // This should pass
        assertDoesNotThrow(() -> listingService.validateListingForBid(listingId));

        listing.setStatus(ListingStatus.DRAFT);
        assertThrows(ResponseStatusException.class, () -> listingService.validateListingForBid(listingId));

        listing.setStatus(ListingStatus.ACTIVE);
        listing.setActivatedAt(Instant.now().minusSeconds(4000));
        assertThrows(ResponseStatusException.class, () -> listingService.validateListingForBid(listingId));
    }

    @Test
    void testSyncPrice() {
        when(listingRepository.syncPrice(listingId, BigDecimal.TEN, 2)).thenReturn(1);
        assertDoesNotThrow(() -> listingService.syncPrice(listingId, BigDecimal.TEN, 2));

        when(listingRepository.syncPrice(listingId, BigDecimal.TEN, 2)).thenReturn(0);
        assertThrows(ResponseStatusException.class, () -> listingService.syncPrice(listingId, BigDecimal.TEN, 2));
    }
}

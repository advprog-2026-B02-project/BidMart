package id.ac.ui.cs.advprog.bidmart.catalog.service;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.model.Listing;
import id.ac.ui.cs.advprog.bidmart.catalog.model.ListingStatus;
import id.ac.ui.cs.advprog.bidmart.catalog.repository.ListingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private ListingServiceImpl listingService;

    private Listing listing;
    private ListingRequest listingRequest;
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
                .images(Collections.emptyList())
                .build();

        listingRequest = ListingRequest.builder()
                .sellerId(sellerId)
                .categoryId(categoryId)
                .title("Test Listing")
                .description("Test Description")
                .status(ListingStatus.DRAFT)
                .startingPrice(new BigDecimal("100.00"))
                .reservePrice(new BigDecimal("150.00"))
                .minimumIncrement(new BigDecimal("10.00"))
                .auctionDuration(3600L)
                .imageUrls(Collections.singletonList("http://example.com/image.jpg"))
                .build();
    }

    @Test
    void createListing_ShouldReturnListingResponse() {
        when(listingRepository.save(any(Listing.class))).thenReturn(listing);

        ListingResponse response = listingService.createListing(listingRequest);

        assertNotNull(response);
        assertEquals(listing.getTitle(), response.getTitle());
        verify(listingRepository, times(1)).save(any(Listing.class));
    }

    @Test
    void getListingById_ShouldReturnListingResponse() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        ListingResponse response = listingService.getListingById(listingId);

        assertNotNull(response);
        assertEquals(listingId, response.getId());
        verify(listingRepository, times(1)).findById(listingId);
    }

    @Test
    void getListingById_ShouldThrowExceptionWhenNotFound() {
        when(listingRepository.findById(listingId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> listingService.getListingById(listingId));
    }
}

package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingSummaryResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private CatalogController catalogController;

    @Test
    void browse() {
        String q = "test";
        UUID category = UUID.randomUUID();
        BigDecimal min = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.TEN;
        Instant endsBefore = Instant.now();
        Pageable pageable = PageRequest.of(0, 10);

        Page<ListingSummaryResponse> page = new PageImpl<>(Collections.emptyList());
        when(listingService.findForCatalog(q, category, min, max, endsBefore, pageable)).thenReturn(page);

        ResponseEntity<Page<ListingSummaryResponse>> res = catalogController.browse(
                q, category, min, max, endsBefore, pageable
        );

        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
        verify(listingService).findForCatalog(q, category, min, max, endsBefore, pageable);
    }

    @Test
    void detail() {
        UUID listingId = UUID.randomUUID();
        ListingDetailResponse detailRes = mock(ListingDetailResponse.class);
        when(detailRes.getTitle()).thenReturn("Title");
        when(listingService.findDetailById(listingId)).thenReturn(detailRes);

        ResponseEntity<ListingDetailResponse> res = catalogController.detail(listingId);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Title", res.getBody().getTitle());
        verify(listingService).findDetailById(listingId);
    }
}

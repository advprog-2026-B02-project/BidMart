package id.ac.ui.cs.advprog.bidmart.catalog.controller;

import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingDetailResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ListingResponse;
import id.ac.ui.cs.advprog.bidmart.catalog.dto.ModerationRequest.ModerateListingRequest;
import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminModerationControllerTest {

    @Mock
    private ListingService listingService;

    @InjectMocks
    private AdminModerationController controller;

    @Test
    void findAll() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<ListingResponse> page = new PageImpl<>(Collections.emptyList());
        when(listingService.findAllForAdmin(pageRequest)).thenReturn(page);

        ResponseEntity<Page<ListingResponse>> res = controller.findAll(pageRequest);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void findById() {
        UUID id = UUID.randomUUID();
        ListingDetailResponse detailRes = mock(ListingDetailResponse.class);
        when(listingService.findDetailById(id)).thenReturn(detailRes);

        ResponseEntity<ListingDetailResponse> res = controller.findById(id);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }

    @Test
    void moderate() {
        UUID id = UUID.randomUUID();
        ModerateListingRequest req = new ModerateListingRequest();
        ListingResponse lr = mock(ListingResponse.class);
        when(listingService.moderate(id, req)).thenReturn(lr);

        ResponseEntity<ListingResponse> res = controller.moderate(id, req);
        assertEquals(200, res.getStatusCode().value());
        assertNotNull(res.getBody());
    }
}

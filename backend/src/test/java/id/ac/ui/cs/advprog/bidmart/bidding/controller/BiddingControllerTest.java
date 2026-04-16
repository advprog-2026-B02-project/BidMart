package id.ac.ui.cs.advprog.bidmart.bidding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.*;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.IdempotencyRecordRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.service.BiddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BiddingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private BiddingService biddingService;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @InjectMocks
    private BiddingController biddingController;

    private UUID auctionId;
    private UUID userId;
    private UUID listingId;

    @BeforeEach
    void setUp() {
        // build MockMvc secara manual tanpa context Spring
        // tambahin CustomArgumentResolver biar paham Pageable
        mockMvc = MockMvcBuilders.standaloneSetup(biddingController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        // setup ObjectMapper manual biar ga error pas baca tipe data waktu (LocalDateTime)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        auctionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        listingId = UUID.randomUUID();
    }

    @Test
    void placeBid_Success_WithIdempotencyKey() throws Exception {
        BidRequestDTO requestDTO = new BidRequestDTO();
        requestDTO.setAmount(new BigDecimal("150000"));

        BidResponseDTO responseDTO = BidResponseDTO.builder()
                .auctionId(auctionId)
                .bidderId(userId)
                .amount(new BigDecimal("150000"))
                .build();

        when(idempotencyRecordRepository.saveAndFlush(any())).thenReturn(null);
        when(biddingService.placeBid(eq(auctionId), eq(userId), any(BidRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/auctions/{auctionId}/bids", auctionId)
                        .header("X-User-Id", userId.toString())
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auctionId").value(auctionId.toString()))
                .andExpect(jsonPath("$.amount").value(150000));

        verify(idempotencyRecordRepository, times(1)).saveAndFlush(any());
        verify(biddingService, times(1)).placeBid(eq(auctionId), eq(userId), any(BidRequestDTO.class));
    }

    @Test
    void placeBid_Conflict_WhenIdempotencyKeyExists() throws Exception {
        BidRequestDTO requestDTO = new BidRequestDTO();
        requestDTO.setAmount(new BigDecimal("150000"));

        when(idempotencyRecordRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        mockMvc.perform(post("/auctions/{auctionId}/bids", auctionId)
                        .header("X-User-Id", userId.toString())
                        .header("Idempotency-Key", "key-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isConflict())
                .andExpect(content().string("permintaan dengan idempotency key ini sedang diproses atau sudah berhasil sebelumnya."));

        verify(biddingService, never()).placeBid(any(), any(), any());
    }

    @Test
    void placeBid_Success_WithoutIdempotencyKey() throws Exception {
        BidRequestDTO requestDTO = new BidRequestDTO();
        requestDTO.setAmount(new BigDecimal("150000"));

        BidResponseDTO responseDTO = BidResponseDTO.builder()
                .auctionId(auctionId)
                .amount(new BigDecimal("150000"))
                .build();

        when(biddingService.placeBid(eq(auctionId), eq(userId), any(BidRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/auctions/{auctionId}/bids", auctionId)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated());

        verify(idempotencyRecordRepository, never()).saveAndFlush(any());
    }

    @Test
    void startAuction_Success() throws Exception {
        AuctionStartRequestDTO requestDTO = new AuctionStartRequestDTO();
        requestDTO.setStartPrice(new BigDecimal("100000"));
        requestDTO.setMinimumIncrement(new BigDecimal("10000"));
        requestDTO.setReservePrice(new BigDecimal("500000"));
        requestDTO.setEndTime(LocalDateTime.now().plusDays(1));

        AuctionResponseDTO responseDTO = AuctionResponseDTO.builder()
                .listingId(listingId)
                .currentPrice(new BigDecimal("100000"))
                .build();

        when(biddingService.startAuction(eq(listingId), any(AuctionStartRequestDTO.class)))
                .thenReturn(responseDTO);

        mockMvc.perform(post("/auctions/listings/{listingId}/start", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").value(listingId.toString()));
    }

    @Test
    void getAuctionStatus_Success() throws Exception {
        AuctionResponseDTO responseDTO = AuctionResponseDTO.builder()
                .id(auctionId)
                .status("ACTIVE")
                .build();

        when(biddingService.getAuctionStatus(auctionId)).thenReturn(responseDTO);

        mockMvc.perform(get("/auctions/{auctionId}", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auctionId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getBidHistory_Success() throws Exception {
        BidResponseDTO bid = BidResponseDTO.builder()
                .bidderId(userId)
                .amount(new BigDecimal("200000"))
                .build();

        Page<BidResponseDTO> pageResponse = new PageImpl<>(List.of(bid), PageRequest.of(0, 10), 1);

        when(biddingService.getBidHistory(eq(auctionId), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/auctions/{auctionId}/bids", auctionId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(200000));
    }

    @Test
    void getAuctionResult_Success() throws Exception {
        AuctionResultDTO resultDTO = AuctionResultDTO.builder()
                .auctionId(auctionId)
                .status("WON")
                .winningBid(new BigDecimal("500000"))
                .build();

        when(biddingService.getAuctionResult(auctionId)).thenReturn(resultDTO);

        mockMvc.perform(get("/auctions/{auctionId}/result", auctionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WON"))
                .andExpect(jsonPath("$.winningBid").value(500000));
    }
}
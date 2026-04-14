package id.ac.ui.cs.advprog.bidmart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.wallet.controller.InternalWalletController;
import id.ac.ui.cs.advprog.bidmart.wallet.controller.WalletExceptionHandler;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InternalWalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private InternalWalletController walletController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    
    private final UUID userId = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new WalletExceptionHandler())
                .build();
    }

    @Test
    void createHoldShouldReturnCreated() throws Exception {
        UUID auctionId = UUID.randomUUID();
        HoldRequest request = HoldRequest.builder()
                .userId(userId)
                .auctionId(auctionId)
                .amount(150_000L)
                .build();

        HoldResponse response = HoldResponse.builder()
                .holdId(UUID.randomUUID())
                .userId(userId)
                .amount(150_000L)
                .status("ACTIVE")
                .build();
        when(walletService.createHold(any())).thenReturn(response);

        mockMvc.perform(post("/internal/wallet/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150_000L));

        verify(walletService).createHold(argThat(req -> req.getAmount() == 150_000L));
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void releaseHoldShouldReturnOk() throws Exception {
        UUID holdId = UUID.randomUUID();
        HoldResponse response = HoldResponse.builder()
                .holdId(holdId)
                .status("RELEASED")
                .build();
        when(walletService.releaseHold(holdId)).thenReturn(response);

        mockMvc.perform(post("/internal/wallet/holds/" + holdId + "/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));

        verify(walletService).releaseHold(holdId);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void captureHoldShouldReturnOk() throws Exception {
        UUID holdId = UUID.randomUUID();
        HoldResponse response = HoldResponse.builder()
                .holdId(holdId)
                .status("CAPTURED")
                .build();
        when(walletService.captureHold(holdId)).thenReturn(response);

        mockMvc.perform(post("/internal/wallet/holds/" + holdId + "/capture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        verify(walletService).captureHold(holdId);
        verifyNoMoreInteractions(walletService);
    }
}

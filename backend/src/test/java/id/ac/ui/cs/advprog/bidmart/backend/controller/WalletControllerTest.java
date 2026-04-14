package id.ac.ui.cs.advprog.bidmart.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import id.ac.ui.cs.advprog.bidmart.wallet.controller.WalletController;
import id.ac.ui.cs.advprog.bidmart.wallet.controller.WalletExceptionHandler;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WalletResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WithdrawRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WithdrawResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final UUID userId = UUID.randomUUID();
    private final String principalSubject = "42";

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new WalletExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(jacksonConverter)
                .build();
    }

    @Test
    void getWalletForCurrentUserShouldUsePrincipal() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        WalletResponse response = WalletResponse.builder()
                .userId(derivedId)
                .availableBalance(900_000L)
                .heldBalance(0L)
                .build();
        when(walletService.getWallet(derivedId)).thenReturn(response);

        mockMvc.perform(get("/api/wallets/me").principal(() -> principalSubject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(900_000L));

        verify(walletService).getWallet(derivedId);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void getWalletShouldReturnResponse() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .userId(userId)
                .availableBalance(1_000_000L)
                .heldBalance(0L)
                .totalBalance(1_000_000L)
                .build();
        when(walletService.getWallet(userId)).thenReturn(response);

        mockMvc.perform(get("/api/wallets/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.availableBalance").value(1_000_000L));

        verify(walletService).getWallet(userId);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void topUpShouldForwardToService() throws Exception {
        TopUpRequest request = TopUpRequest.builder().amount(200_000L).build();
        WalletResponse response = WalletResponse.builder()
                .userId(userId)
                .availableBalance(1_200_000L)
                .heldBalance(0L)
                .totalBalance(1_200_000L)
                .build();
        when(walletService.topUp(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/wallets/" + userId + "/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(1_200_000L));

        verify(walletService).topUp(eq(userId), argThat(req -> req.getAmount() == 200_000L));
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void topUpForCurrentUserShouldForwardToService() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        TopUpRequest request = TopUpRequest.builder().amount(50_000L).build();
        WalletResponse response = WalletResponse.builder()
                .userId(derivedId)
                .availableBalance(50_000L)
                .heldBalance(0L)
                .build();
        when(walletService.topUp(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/wallets/me/top-up")
                        .principal(() -> principalSubject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(50_000L));

        verify(walletService).topUp(eq(derivedId), argThat(req -> req.getAmount() == 50_000L));
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void resetShouldCallServiceAndReturnWallet() throws Exception {
        WalletResponse response = WalletResponse.builder()
                .userId(userId)
                .availableBalance(0L)
                .heldBalance(0L)
                .build();
        when(walletService.resetWallet(userId)).thenReturn(response);

        mockMvc.perform(post("/api/wallets/" + userId + "/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(0L));

        verify(walletService).resetWallet(userId);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void resetCurrentUserShouldCallService() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        WalletResponse response = WalletResponse.builder()
                .userId(derivedId)
                .availableBalance(0L)
                .heldBalance(0L)
                .build();
        when(walletService.resetWallet(derivedId)).thenReturn(response);

        mockMvc.perform(post("/api/wallets/me/reset").principal(() -> principalSubject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heldBalance").value(0L));

        verify(walletService).resetWallet(derivedId);
        verifyNoMoreInteractions(walletService);
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

        mockMvc.perform(post("/api/wallets/" + userId + "/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(150_000L));

        verify(walletService).createHold(argThat(req -> req.getAmount() == 150_000L));
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void createHoldShouldFailOnUserMismatch() throws Exception {
        HoldRequest request = HoldRequest.builder()
                .userId(UUID.randomUUID())
                .auctionId(UUID.randomUUID())
                .amount(100_000L)
                .build();

        mockMvc.perform(post("/api/wallets/" + userId + "/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createHoldForCurrentUserShouldPopulateUserId() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        HoldRequest request = HoldRequest.builder()
                .auctionId(UUID.randomUUID())
                .amount(75_000L)
                .build();
        HoldResponse response = HoldResponse.builder()
                .holdId(UUID.randomUUID())
                .userId(derivedId)
                .amount(75_000L)
                .status("ACTIVE")
                .build();
        when(walletService.createHold(any())).thenReturn(response);

        mockMvc.perform(post("/api/wallets/me/holds")
                        .principal(() -> principalSubject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(75_000L));

        verify(walletService).createHold(argThat(req ->
                derivedId.equals(req.getUserId()) && req.getAmount() == 75_000L));
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

        mockMvc.perform(post("/api/wallets/holds/" + holdId + "/release"))
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

        mockMvc.perform(post("/api/wallets/holds/" + holdId + "/capture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        verify(walletService).captureHold(holdId);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void withdrawShouldReturnCreated() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        WithdrawRequest request = WithdrawRequest.builder()
                .amount(500_000L)
                .bankCode("BCA")
                .accountNumber("1234567890")
                .accountName("Test User")
                .build();
        WithdrawResponse response = WithdrawResponse.builder()
                .transactionId(UUID.randomUUID())
                .amount(500_000L)
                .fee(5_000L)
                .netAmount(495_000L)
                .status("PROCESSING")
                .estimatedCompletion(LocalDateTime.now().plusDays(1))
                .build();
        when(walletService.withdraw(eq(derivedId), any())).thenReturn(response);

        mockMvc.perform(post("/api/wallets/me/withdraw")
                        .principal(() -> principalSubject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(500_000L))
                .andExpect(jsonPath("$.fee").value(5_000L))
                .andExpect(jsonPath("$.netAmount").value(495_000L))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(walletService).withdraw(eq(derivedId), argThat(req -> req.getAmount() == 500_000L));
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void getTransactionHistoryShouldReturnOk() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        TransactionResponse txn = TransactionResponse.builder()
                .id(UUID.randomUUID())
                .type("TOPUP")
                .amount(200_000L)
                .description("Top-up saldo")
                .balanceAfter(1_200_000L)
                .createdAt(LocalDateTime.now())
                .build();
        when(walletService.getTransactionHistory(eq(derivedId), any()))
                .thenReturn(new PageImpl<>(List.of(txn), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/wallets/me/transactions")
                        .principal(() -> principalSubject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TOPUP"))
                .andExpect(jsonPath("$.content[0].amount").value(200_000L));

        verify(walletService).getTransactionHistory(eq(derivedId), any());
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void getTransactionByIdShouldReturnOk() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        UUID txnId = UUID.randomUUID();
        TransactionResponse txn = TransactionResponse.builder()
                .id(txnId)
                .type("WITHDRAW")
                .amount(-505_000L)
                .description("Penarikan ke BCA")
                .balanceAfter(495_000L)
                .createdAt(LocalDateTime.now())
                .build();
        when(walletService.getTransaction(eq(derivedId), eq(txnId))).thenReturn(txn);

        mockMvc.perform(get("/api/wallets/me/transactions/" + txnId)
                        .principal(() -> principalSubject))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(txnId.toString()))
                .andExpect(jsonPath("$.type").value("WITHDRAW"));

        verify(walletService).getTransaction(eq(derivedId), eq(txnId));
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void getTransactionByIdNotFoundShouldReturn404() throws Exception {
        UUID derivedId = derivedUuid(principalSubject);
        UUID txnId = UUID.randomUUID();
        when(walletService.getTransaction(eq(derivedId), eq(txnId)))
                .thenThrow(new IllegalArgumentException("Transaction not found: " + txnId));

        mockMvc.perform(get("/api/wallets/me/transactions/" + txnId)
                        .principal(() -> principalSubject))
                .andExpect(status().isNotFound());

        verify(walletService).getTransaction(eq(derivedId), eq(txnId));
        verifyNoMoreInteractions(walletService);
    }

    private UUID derivedUuid(String subject) {
        return UUID.nameUUIDFromBytes(("wallet-user-" + subject).getBytes(StandardCharsets.UTF_8));
    }
}

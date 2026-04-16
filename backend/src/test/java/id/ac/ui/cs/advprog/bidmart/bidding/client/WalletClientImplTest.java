package id.ac.ui.cs.advprog.bidmart.bidding.client;

import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletClientImplTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletClientImpl walletClient;

    @Captor
    private ArgumentCaptor<HoldRequest> holdRequestCaptor;

    private UUID userId;
    private UUID auctionId;
    private UUID holdId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
        holdId = UUID.randomUUID();
    }

    @Test
    void holdFunds_Success_ConvertsBigDecimalToLong() {
        // tes pakai angka desimal buat mastiin beneran kepotong
        // ini cuman cek tipe data beneran keubah
        BigDecimal amount = new BigDecimal("150000.75");

        HoldResponse mockResponse = new HoldResponse();
        mockResponse.setHoldId(holdId);

        when(walletService.createHold(any(HoldRequest.class))).thenReturn(mockResponse);

        UUID result = walletClient.holdFunds(userId, auctionId, amount);

        // pastiin balikan UUID-nya cocok
        assertEquals(holdId, result);

        // verify konversi payload
        verify(walletService).createHold(holdRequestCaptor.capture());
        HoldRequest captured = holdRequestCaptor.getValue();

        assertEquals(userId, captured.getUserId());
        assertEquals(auctionId, captured.getAuctionId());

        // desimal .75-nya harus ilang
        assertEquals(150000L, captured.getAmount());
    }

    @Test
    void releaseFunds_Success() {
        walletClient.releaseFunds(holdId);
        verify(walletService).releaseHold(holdId);
    }

    @Test
    void captureWinnerFunds_Success() {
        walletClient.captureWinnerFunds(auctionId, userId);
        verify(walletService).captureWinnerHold(auctionId, userId);
    }

    @Test
    void releaseAllAuctionHolds_Success() {
        walletClient.releaseAllAuctionHolds(auctionId);
        verify(walletService).releaseAllHoldsForAuction(auctionId);
    }
}
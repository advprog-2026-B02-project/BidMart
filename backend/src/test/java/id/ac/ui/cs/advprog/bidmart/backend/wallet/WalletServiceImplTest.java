package id.ac.ui.cs.advprog.bidmart.backend.wallet;

import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WalletResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.model.BalanceHold;
import id.ac.ui.cs.advprog.bidmart.wallet.model.HoldStatus;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.BalanceHoldRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.WalletRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.WalletTransactionRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletTransactionRepository transactionRepository;

    @Mock
    private BalanceHoldRepository holdRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    private UUID userId;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .availableBalance(1000000) 
                .heldBalance(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    

    @Test
    void getWalletShouldReturnExistingWallet() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWallet(userId);

        assertEquals(1000000, response.getAvailableBalance());
        assertEquals(0, response.getHeldBalance());
        assertEquals(1000000, response.getTotalBalance());
    }

    @Test
    void getWalletShouldCreateNewIfNotExists() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });

        WalletResponse response = walletService.getWallet(userId);

        assertEquals(0, response.getAvailableBalance());
        verify(walletRepository).save(any(Wallet.class));
    }

    

    @Test
    void topUpShouldIncreaseBalance() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        TopUpRequest request = TopUpRequest.builder().amount(500000).build();
        WalletResponse response = walletService.topUp(userId, request);

        assertEquals(1500000, response.getAvailableBalance());
        verify(transactionRepository).save(any());
    }

    @Test
    void resetWalletShouldClearBalances() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(holdRepository.findAllByWalletId(wallet.getId())).thenReturn(List.of());

        WalletResponse response = walletService.resetWallet(userId);

        assertEquals(0, response.getAvailableBalance());
        assertEquals(0, response.getHeldBalance());
    }

    

    @Test
    void createHoldShouldMoveBalanceFromAvailableToHeld() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(holdRepository.findByUserIdAndAuctionIdAndStatus(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(holdRepository.save(any(BalanceHold.class))).thenAnswer(inv -> {
            BalanceHold h = inv.getArgument(0);
            h.setId(UUID.randomUUID());
            return h;
        });

        HoldRequest request = HoldRequest.builder()
                .userId(userId)
                .auctionId(UUID.randomUUID())
                .amount(300000)
                .build();

        HoldResponse response = walletService.createHold(request);

        assertNotNull(response.getHoldId());
        assertEquals(300000, response.getAmount());
        assertEquals("ACTIVE", response.getStatus());
        
        assertEquals(700000, wallet.getAvailableBalance());
        assertEquals(300000, wallet.getHeldBalance());
    }

    @Test
    void createHoldShouldFailWhenInsufficientBalance() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        HoldRequest request = HoldRequest.builder()
                .userId(userId)
                .auctionId(UUID.randomUUID())
                .amount(5000000) 
                .build();

        assertThrows(IllegalStateException.class,
                () -> walletService.createHold(request));
    }

    

    @Test
    void releaseHoldShouldMoveBalanceBackToAvailable() {
        wallet.setAvailableBalance(700000);
        wallet.setHeldBalance(300000);

        UUID holdId = UUID.randomUUID();
        BalanceHold hold = BalanceHold.builder()
                .id(holdId)
                .walletId(wallet.getId())
                .userId(userId)
                .amount(300000)
                .status(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        HoldResponse response = walletService.releaseHold(holdId);

        assertEquals("RELEASED", response.getStatus());
        assertEquals(1000000, wallet.getAvailableBalance()); 
        assertEquals(0, wallet.getHeldBalance());
    }

    

    @Test
    void captureHoldShouldDeductFromHeldBalance() {
        wallet.setAvailableBalance(700000);
        wallet.setHeldBalance(300000);

        UUID holdId = UUID.randomUUID();
        BalanceHold hold = BalanceHold.builder()
                .id(holdId)
                .walletId(wallet.getId())
                .userId(userId)
                .amount(300000)
                .status(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        HoldResponse response = walletService.captureHold(holdId);

        assertEquals("CAPTURED", response.getStatus());
        assertEquals(700000, wallet.getAvailableBalance()); 
        assertEquals(0, wallet.getHeldBalance()); 
    }

    @Test
    void captureHoldShouldFailWhenNotActive() {
        UUID holdId = UUID.randomUUID();
        BalanceHold hold = BalanceHold.builder()
                .id(holdId)
                .status(HoldStatus.RELEASED) 
                .build();

        when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));

        assertThrows(IllegalStateException.class,
                () -> walletService.captureHold(holdId));
    }
}

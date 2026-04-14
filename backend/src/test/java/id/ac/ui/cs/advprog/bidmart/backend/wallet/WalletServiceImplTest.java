package id.ac.ui.cs.advprog.bidmart.backend.wallet;

import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WalletResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WithdrawRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WithdrawResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.model.BalanceHold;
import id.ac.ui.cs.advprog.bidmart.wallet.model.HoldStatus;
import id.ac.ui.cs.advprog.bidmart.wallet.model.TransactionType;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;
import id.ac.ui.cs.advprog.bidmart.wallet.model.WalletTransaction;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
    void resetWalletShouldReleaseActiveHolds() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        
        BalanceHold hold = BalanceHold.builder()
                .status(HoldStatus.ACTIVE)
                .amount(100L)
                .build();
        when(holdRepository.findAllByWalletId(wallet.getId())).thenReturn(List.of(hold));

        walletService.resetWallet(userId);
        assertEquals(HoldStatus.RELEASED, hold.getStatus());
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
                .amount(300000L)
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
    void releaseHold_NotFound() {
        UUID holdId = UUID.randomUUID();
        when(holdRepository.findById(holdId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> walletService.releaseHold(holdId));
    }

    @Test
    void releaseHold_NotActive() {
        UUID holdId = UUID.randomUUID();
        BalanceHold hold = BalanceHold.builder().status(HoldStatus.RELEASED).build();
        when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
        assertThrows(IllegalStateException.class, () -> walletService.releaseHold(holdId));
    }

    @Test
    void releaseHold_WalletNotFound() {
        UUID holdId = UUID.randomUUID();
        BalanceHold hold = BalanceHold.builder().status(HoldStatus.ACTIVE).walletId(UUID.randomUUID()).build();
        when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
        when(walletRepository.findById(hold.getWalletId())).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> walletService.releaseHold(holdId));
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

    @Test
    void captureHold_NotFound() {
        UUID holdId = UUID.randomUUID();
        when(holdRepository.findById(holdId)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> walletService.captureHold(holdId));
    }

    @Test
    void captureHold_WalletNotFound() {
        UUID holdId = UUID.randomUUID();
        BalanceHold hold = BalanceHold.builder().status(HoldStatus.ACTIVE).walletId(UUID.randomUUID()).build();
        when(holdRepository.findById(holdId)).thenReturn(Optional.of(hold));
        when(walletRepository.findById(hold.getWalletId())).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> walletService.captureHold(holdId));
    }

    // --- Withdraw ---

    @Test
    void withdraw_ShouldDeductBalanceAndReturnResponse() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletTransaction savedTxn = WalletTransaction.builder()
                .id(UUID.randomUUID())
                .walletId(wallet.getId())
                .type(TransactionType.WITHDRAW)
                .amount(-505000L)
                .balanceAfter(495000L)
                .createdAt(LocalDateTime.now())
                .build();
        when(transactionRepository.save(any())).thenReturn(savedTxn);

        WithdrawRequest request = WithdrawRequest.builder()
                .amount(500000L)
                .bankCode("BCA")
                .accountNumber("1234567890")
                .accountName("Test User")
                .build();

        WithdrawResponse response = walletService.withdraw(userId, request);

        assertNotNull(response.getTransactionId());
        assertEquals(500000L, response.getAmount());
        assertEquals(5000L, response.getFee());
        assertEquals(495000L, response.getNetAmount());
        assertEquals("PROCESSING", response.getStatus());
        assertEquals(495000L, wallet.getAvailableBalance());
    }

    @Test
    void withdraw_ShouldFailWhenInsufficientBalance() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WithdrawRequest request = WithdrawRequest.builder()
                .amount(1000000L)
                .bankCode("BCA")
                .accountNumber("1234567890")
                .accountName("Test User")
                .build();

        assertThrows(IllegalStateException.class, () -> walletService.withdraw(userId, request));
    }

    // --- Transaction History ---

    @Test
    void getTransactionHistory_ShouldReturnPagedResults() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        WalletTransaction txn = WalletTransaction.builder()
                .id(UUID.randomUUID())
                .walletId(wallet.getId())
                .type(TransactionType.TOPUP)
                .amount(500000L)
                .balanceAfter(1500000L)
                .description("Top-up saldo")
                .createdAt(LocalDateTime.now())
                .build();
        Page<WalletTransaction> page = new PageImpl<>(List.of(txn));
        when(transactionRepository.findByWalletIdOrderByCreatedAtDesc(eq(wallet.getId()), any()))
                .thenReturn(page);

        Page<TransactionResponse> result = walletService.getTransactionHistory(userId, PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals("TOPUP", result.getContent().get(0).getType());
        assertEquals(500000L, result.getContent().get(0).getAmount());
    }

    @Test
    void getTransaction_ShouldReturnTransactionForOwner() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        UUID txnId = UUID.randomUUID();
        WalletTransaction txn = WalletTransaction.builder()
                .id(txnId)
                .walletId(wallet.getId())
                .type(TransactionType.TOPUP)
                .amount(200000L)
                .balanceAfter(1200000L)
                .description("Top-up saldo")
                .createdAt(LocalDateTime.now())
                .build();
        when(transactionRepository.findById(txnId)).thenReturn(Optional.of(txn));

        TransactionResponse result = walletService.getTransaction(userId, txnId);

        assertEquals(txnId, result.getId());
        assertEquals("TOPUP", result.getType());
    }

    @Test
    void getTransaction_ShouldThrowWhenNotFound() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
        UUID txnId = UUID.randomUUID();
        when(transactionRepository.findById(txnId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> walletService.getTransaction(userId, txnId));
    }

    @Test
    void getTransaction_ShouldThrowWhenBelongsToDifferentWallet() {
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        UUID txnId = UUID.randomUUID();
        WalletTransaction txn = WalletTransaction.builder()
                .id(txnId)
                .walletId(UUID.randomUUID()) // different wallet
                .type(TransactionType.TOPUP)
                .amount(200000L)
                .balanceAfter(200000L)
                .createdAt(LocalDateTime.now())
                .build();
        when(transactionRepository.findById(txnId)).thenReturn(Optional.of(txn));

        assertThrows(IllegalArgumentException.class,
                () -> walletService.getTransaction(userId, txnId));
    }

    // --- Auction Event Handling ---

    @Test
    void captureWinnerHold_ShouldCaptureWinnerAndReleaseOthers() {
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        UUID loserId = UUID.randomUUID();

        Wallet winnerWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(winnerId)
                .availableBalance(0L)
                .heldBalance(500000L)
                .createdAt(LocalDateTime.now())
                .build();
        Wallet loserWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .userId(loserId)
                .availableBalance(0L)
                .heldBalance(300000L)
                .createdAt(LocalDateTime.now())
                .build();

        BalanceHold winnerHold = BalanceHold.builder()
                .id(UUID.randomUUID())
                .walletId(winnerWallet.getId())
                .userId(winnerId)
                .auctionId(auctionId)
                .amount(500000L)
                .status(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        BalanceHold loserHold = BalanceHold.builder()
                .id(UUID.randomUUID())
                .walletId(loserWallet.getId())
                .userId(loserId)
                .auctionId(auctionId)
                .amount(300000L)
                .status(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        when(holdRepository.findByAuctionIdAndStatus(auctionId, HoldStatus.ACTIVE))
                .thenReturn(List.of(winnerHold, loserHold));
        when(walletRepository.findById(winnerWallet.getId())).thenReturn(Optional.of(winnerWallet));
        when(walletRepository.findById(loserWallet.getId())).thenReturn(Optional.of(loserWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.captureWinnerHold(auctionId, winnerId);

        assertEquals(HoldStatus.CAPTURED, winnerHold.getStatus());
        assertEquals(0L, winnerWallet.getHeldBalance());

        assertEquals(HoldStatus.RELEASED, loserHold.getStatus());
        assertEquals(300000L, loserWallet.getAvailableBalance());
        assertEquals(0L, loserWallet.getHeldBalance());

        verify(walletRepository, times(2)).save(any(Wallet.class));
    }

    @Test
    void releaseAllHoldsForAuction_ShouldReleaseAllActiveHolds() {
        UUID auctionId = UUID.randomUUID();

        Wallet wallet1 = Wallet.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .availableBalance(0L).heldBalance(200000L)
                .createdAt(LocalDateTime.now()).build();
        Wallet wallet2 = Wallet.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID())
                .availableBalance(100000L).heldBalance(150000L)
                .createdAt(LocalDateTime.now()).build();

        BalanceHold hold1 = BalanceHold.builder()
                .id(UUID.randomUUID()).walletId(wallet1.getId()).userId(wallet1.getUserId())
                .auctionId(auctionId).amount(200000L).status(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();
        BalanceHold hold2 = BalanceHold.builder()
                .id(UUID.randomUUID()).walletId(wallet2.getId()).userId(wallet2.getUserId())
                .auctionId(auctionId).amount(150000L).status(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).build();

        when(holdRepository.findByAuctionIdAndStatus(auctionId, HoldStatus.ACTIVE))
                .thenReturn(List.of(hold1, hold2));
        when(walletRepository.findById(wallet1.getId())).thenReturn(Optional.of(wallet1));
        when(walletRepository.findById(wallet2.getId())).thenReturn(Optional.of(wallet2));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        walletService.releaseAllHoldsForAuction(auctionId);

        assertEquals(HoldStatus.RELEASED, hold1.getStatus());
        assertEquals(200000L, wallet1.getAvailableBalance());
        assertEquals(0L, wallet1.getHeldBalance());

        assertEquals(HoldStatus.RELEASED, hold2.getStatus());
        assertEquals(250000L, wallet2.getAvailableBalance());
        assertEquals(0L, wallet2.getHeldBalance());

        verify(walletRepository, times(2)).save(any(Wallet.class));
    }
}

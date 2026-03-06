package id.ac.ui.cs.advprog.bidmart.wallet.service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WalletResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.model.BalanceHold;
import id.ac.ui.cs.advprog.bidmart.wallet.model.HoldStatus;
import id.ac.ui.cs.advprog.bidmart.wallet.model.TransactionType;
import id.ac.ui.cs.advprog.bidmart.wallet.model.Wallet;
import id.ac.ui.cs.advprog.bidmart.wallet.model.WalletTransaction;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.BalanceHoldRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.WalletRepository;
import id.ac.ui.cs.advprog.bidmart.wallet.repository.WalletTransactionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class WalletServiceImpl implements WalletService {
    private WalletRepository walletRepository;
    private BalanceHoldRepository balanceHoldRepository;
    private WalletTransactionRepository walletTransactionRepository;
    
    public WalletServiceImpl(WalletRepository walletRepository,
                             BalanceHoldRepository balanceHoldRepository,
                             WalletTransactionRepository walletTransactionRepository
    ){
        this.walletRepository=walletRepository;
        this.balanceHoldRepository=balanceHoldRepository;
        this.walletTransactionRepository=walletTransactionRepository;
    }

    @Override
    public WalletResponse getWallet(UUID userId) {
        return walletToWalletResponse(findOrCreateWallet(userId));

    }

    @Override
    public WalletResponse topUp(UUID userId, TopUpRequest request) {
        Wallet wallet = findOrCreateWallet(userId);
        wallet.setAvailableBalance(wallet.getAvailableBalance() + request.getAmount());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);
        saveTransaction(wallet, TransactionType.TOPUP,
                         "Top-up saldo", request.getAmount(), null);

        return walletToWalletResponse(wallet);
    }

    @Transactional // sementara buat demo cara kerja wallet di fe
    public WalletResponse resetWallet(UUID userId) {
        Wallet wallet = findOrCreateWallet(userId);
        List<BalanceHold> holds = balanceHoldRepository.findAllByWalletId(wallet.getId());
        holds.stream()
                .filter(hold -> hold.getStatus() == HoldStatus.ACTIVE)
                .forEach(hold -> releaseHoldInternal(wallet, hold));

        wallet.setAvailableBalance(0);
        wallet.setHeldBalance(0);
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        return walletToWalletResponse(wallet);
    }

    @Override
    @Transactional
    public HoldResponse createHold(HoldRequest request) {
        Wallet wallet = findOrCreateWallet(request.getUserId());

        // Cek saldo cukup
        if (wallet.getAvailableBalance() < request.getAmount()) {
            throw new IllegalStateException(
                    String.format("Saldo tidak mencukupi. Tersedia: %d, Dibutuhkan: %d",
                            wallet.getAvailableBalance(), request.getAmount()));
        }
        
        balanceHoldRepository.findByUserIdAndAuctionIdAndStatus(
                request.getUserId(), request.getAuctionId(), HoldStatus.ACTIVE)
                .ifPresent(existingHold -> releaseHoldInternal(wallet, existingHold));

        wallet.setAvailableBalance(wallet.getAvailableBalance() - request.getAmount());
        wallet.setHeldBalance(wallet.getHeldBalance() + request.getAmount());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        BalanceHold hold = BalanceHold.builder()
                .walletId(wallet.getId())
                .userId(request.getUserId())
                .auctionId(request.getAuctionId())
                .bidId(request.getBidId())
                .amount(request.getAmount())
                .status(HoldStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        balanceHoldRepository.save(hold);

        saveTransaction(wallet, TransactionType.HOLD,"Hold balance for bid",
                -request.getAmount(),  request.getAuctionId());

        return toHoldResponse(hold);
    }

    @Override
    public HoldResponse releaseHold(UUID holdId) {
        BalanceHold hold = balanceHoldRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active: " + hold.getStatus());
        }

        Wallet wallet = walletRepository.findById(hold.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));

        releaseHoldInternal(wallet, hold);
        walletRepository.save(wallet);

        return toHoldResponse(hold);
    }

    @Override
    public HoldResponse captureHold(UUID holdId) {
        BalanceHold hold = balanceHoldRepository.findById(holdId)
                .orElseThrow(() -> new IllegalArgumentException("Hold not found: " + holdId));

        if (hold.getStatus() != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Hold is not active: " + hold.getStatus());
        }

        Wallet wallet = walletRepository.findById(hold.getWalletId())
                .orElseThrow(() -> new IllegalStateException("Wallet not found"));
        wallet.setHeldBalance(wallet.getHeldBalance() - hold.getAmount());
        wallet.setUpdatedAt(LocalDateTime.now());
        walletRepository.save(wallet);

        hold.setStatus(HoldStatus.CAPTURED);
        hold.setUpdatedAt(LocalDateTime.now());
        balanceHoldRepository.save(hold);

        saveTransaction(wallet, TransactionType.CAPTURE,"bid payment",
                -hold.getAmount(),  hold.getAuctionId());

        return toHoldResponse(hold);
    }
    public Wallet findOrCreateWallet(UUID userId){
        Optional<Wallet> wallet = walletRepository.findByUserId(userId);
        if (wallet.isEmpty()){
             Wallet newWallet = Wallet.builder()
                            .userId(userId)
                            .availableBalance(0)
                            .heldBalance(0)
                            .createdAt(LocalDateTime.now())
                            .build();
            return walletRepository.save(newWallet);
        }
        return wallet.get();
    }

     private void releaseHoldInternal(Wallet wallet, BalanceHold hold) {
        wallet.setAvailableBalance(wallet.getAvailableBalance()+hold.getAmount());
        wallet.setHeldBalance(wallet.getHeldBalance()-hold.getAmount());
        wallet.setUpdatedAt(LocalDateTime.now());

        hold.setStatus(HoldStatus.RELEASED);
        balanceHoldRepository.save(hold);
        saveTransaction(wallet, TransactionType.RELEASE, "Release hold, lost bid", hold.getAmount(), hold.getAuctionId());

    }
    public void saveTransaction(Wallet wallet, TransactionType type, String description, 
                                Long amount, UUID referenceId){
        WalletTransaction transaction = WalletTransaction.builder()
                .walletId(wallet.getId())
                .type(type)
                .amount(amount)
                .balanceAfter(wallet.getAvailableBalance())
                .description(description)
                .referenceId(referenceId)
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(transaction);
    }
    public WalletResponse walletToWalletResponse(Wallet wallet){
        WalletResponse walletResponse = new WalletResponse();
        walletResponse.setAvailableBalance(wallet.getAvailableBalance());
        walletResponse.setHeldBalance(wallet.getHeldBalance());
        walletResponse.setUserId(wallet.getUserId());
        walletResponse.setTotalBalance(wallet.getTotalBalance());
        walletResponse.setUpdatedAt(wallet.getUpdatedAt());
        return walletResponse;
    }


    private HoldResponse toHoldResponse(BalanceHold hold) {
        return HoldResponse.builder()
                .holdId(hold.getId())
                .userId(hold.getUserId())
                .amount(hold.getAmount())
                .status(hold.getStatus().name())
                .createdAt(hold.getCreatedAt())
                .build();
    }
}

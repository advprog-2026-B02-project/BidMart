package id.ac.ui.cs.advprog.bidmart.wallet.service;

import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TransactionResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WalletResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WithdrawRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WithdrawResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WalletService {

    WalletResponse getWallet(UUID userId);
    WalletResponse topUp(UUID userId, TopUpRequest request);
    WalletResponse resetWallet(UUID userId); // sementara

    WithdrawResponse withdraw(UUID userId, WithdrawRequest request);

    Page<TransactionResponse> getTransactionHistory(UUID userId, Pageable pageable);
    TransactionResponse getTransaction(UUID userId, UUID transactionId);

    HoldResponse createHold(HoldRequest request);
    HoldResponse releaseHold(UUID holdId);
    HoldResponse captureHold(UUID holdId);

    void captureWinnerHold(UUID auctionId, UUID winnerId);
    void releaseAllHoldsForAuction(UUID auctionId);
}

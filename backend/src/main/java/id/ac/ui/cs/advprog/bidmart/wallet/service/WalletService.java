package id.ac.ui.cs.advprog.bidmart.wallet.service;

import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WalletResponse;

import java.util.UUID;

public interface WalletService {

    WalletResponse getWallet(UUID userId);
    WalletResponse topUp(UUID userId, TopUpRequest request);

    HoldResponse createHold(HoldRequest request);
    HoldResponse releaseHold(UUID holdId);
    HoldResponse captureHold(UUID holdId);
}
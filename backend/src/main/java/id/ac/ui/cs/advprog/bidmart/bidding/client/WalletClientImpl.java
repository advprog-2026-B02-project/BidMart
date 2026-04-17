package id.ac.ui.cs.advprog.bidmart.bidding.client;

import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WalletClientImpl implements WalletClient {

    private final WalletService walletService;

    @Override
    public UUID holdFunds(UUID userId, UUID auctionId, BigDecimal amount) {
        HoldRequest request = new HoldRequest();
        request.setUserId(userId);
        request.setAuctionId(auctionId);

        // konversi bigdecimal dari bidding menjadi long untuk wallet
        request.setAmount(amount.longValue());

        HoldResponse response = walletService.createHold(request);

        return response.getHoldId();
    }

    @Override
    public void releaseFunds(UUID holdId) {
        walletService.releaseHold(holdId);
    }

    @Override
    public void captureWinnerFunds(UUID auctionId, UUID winnerId) {
        // panggil method bawaan wallet service untuk memotong dana pemenang
        walletService.captureWinnerHold(auctionId, winnerId);
    }

    @Override
    public void releaseAllAuctionHolds(UUID auctionId) {
        // panggil method bawaan wallet service untuk melepas semua dana jika lelang batal
        walletService.releaseAllHoldsForAuction(auctionId);
    }
}
package id.ac.ui.cs.advprog.bidmart.wallet.event;

import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.WinnerDeterminedEvent;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class WalletAuctionEventListener {

    private final WalletService walletService;

    public WalletAuctionEventListener(WalletService walletService) {
        this.walletService = walletService;
    }

    @EventListener
    public void onWinnerDetermined(WinnerDeterminedEvent event) {
        walletService.captureWinnerHold(event.getAuctionId(), event.getWinnerId());
    }

    @EventListener
    public void onAuctionUnsold(AuctionUnsoldEvent event) {
        walletService.releaseAllHoldsForAuction(event.getAuctionId());
    }
}

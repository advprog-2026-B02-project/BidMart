package id.ac.ui.cs.advprog.bidmart.backend.wallet;

import id.ac.ui.cs.advprog.bidmart.common.event.AuctionUnsoldEvent;
import id.ac.ui.cs.advprog.bidmart.common.event.WinnerDeterminedEvent;
import id.ac.ui.cs.advprog.bidmart.wallet.event.WalletAuctionEventListener;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class WalletAuctionEventListenerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletAuctionEventListener listener;

    @Test
    void onWinnerDetermined_ShouldCallCaptureWinnerHold() {
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        WinnerDeterminedEvent event = new WinnerDeterminedEvent(auctionId, winnerId, 9_500_000L);

        listener.onWinnerDetermined(event);

        verify(walletService).captureWinnerHold(auctionId, winnerId);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void onAuctionUnsold_ShouldCallReleaseAllHolds() {
        UUID auctionId = UUID.randomUUID();
        AuctionUnsoldEvent event = new AuctionUnsoldEvent(auctionId);

        listener.onAuctionUnsold(event);

        verify(walletService).releaseAllHoldsForAuction(auctionId);
        verifyNoMoreInteractions(walletService);
    }
}

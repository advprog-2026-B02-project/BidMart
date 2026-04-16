package id.ac.ui.cs.advprog.bidmart.bidding.scheduler;

import id.ac.ui.cs.advprog.bidmart.bidding.service.BiddingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BiddingSchedulerTest {

    @Mock
    private BiddingService biddingService;

    @InjectMocks
    private BiddingScheduler biddingScheduler;

    @Test
    void checkAndCloseExpiredAuctions_Success() {
        // eksekusi method scheduler
        biddingScheduler.checkAndCloseExpiredAuctions();

        // verifikasi service beneran dipanggil
        verify(biddingService, times(1)).closeExpiredAuctions();
    }

    @Test
    void checkAndCloseExpiredAuctions_CatchException_DoesNotCrash() {
        // simulasi error database atau logic di dalem service
        doThrow(new RuntimeException("Database timeout")).when(biddingService).closeExpiredAuctions();

        // eksekusi method scheduler (harusnya tidak throw exception / hijau karena ada try-catch)
        biddingScheduler.checkAndCloseExpiredAuctions();

        // verifikasi service dipanggil
        verify(biddingService, times(1)).closeExpiredAuctions();
    }
}
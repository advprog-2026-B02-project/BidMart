package id.ac.ui.cs.advprog.bidmart.bidding.scheduler;

import id.ac.ui.cs.advprog.bidmart.bidding.service.BiddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiddingScheduler {

    private final BiddingService biddingService;

    // mengecek lelang yang kedaluwarsa setiap 10 detik
    @Scheduled(fixedRate = 10000)
    public void checkAndCloseExpiredAuctions() {
        try {
            biddingService.closeExpiredAuctions();
        } catch (Exception e) {
            log.error("terjadi kesalahan saat menutup lelang yang kedaluwarsa", e);
        }
    }
}
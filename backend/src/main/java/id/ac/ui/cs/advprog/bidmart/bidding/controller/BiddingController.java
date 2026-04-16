package id.ac.ui.cs.advprog.bidmart.bidding.controller;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.*;
import id.ac.ui.cs.advprog.bidmart.bidding.model.IdempotencyRecord;
import id.ac.ui.cs.advprog.bidmart.bidding.repository.IdempotencyRecordRepository;
import id.ac.ui.cs.advprog.bidmart.bidding.service.BiddingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/auctions")
@RequiredArgsConstructor
public class BiddingController {

    private final BiddingService biddingService;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    // menambahkan idempotency-key untuk mencegah double charge jika koneksi lag
    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<?> placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-User-Id") UUID bidderId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BidRequestDTO requestDTO
    ) {
        // pengecekan idempotency untuk mencegah duplikasi penawaran
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            try {
                IdempotencyRecord idempotencyRecord = IdempotencyRecord.builder()
                        .idempotencyKey(idempotencyKey)
                        .userId(bidderId)
                        .requestPath("/auctions/" + auctionId + "/bids")
                        .createdAt(LocalDateTime.now())
                        .build();

                // saveandflush akan langsung memicu error jika key sudah ada di database
                idempotencyRecordRepository.saveAndFlush(idempotencyRecord);
            } catch (DataIntegrityViolationException e) {
                // jika terjadi error duplikasi, tolak request dengan status 409 conflict
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("permintaan dengan idempotency key ini sedang diproses atau sudah berhasil sebelumnya.");
            }
        }

        BidResponseDTO response = biddingService.placeBid(auctionId, bidderId, requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // mengaktifkan lelang baru (biasanya dipanggil oleh penjual atau trigger dari modul catalog)
    @PostMapping("/listings/{listingId}/start")
    public ResponseEntity<AuctionResponseDTO> startAuction(
            @PathVariable UUID listingId,
            @Valid @RequestBody AuctionStartRequestDTO request
    ) {
        AuctionResponseDTO response = biddingService.startAuction(listingId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // mengambil status lelang terkini
    @GetMapping("/{auctionId}")
    public ResponseEntity<AuctionResponseDTO> getAuctionStatus(@PathVariable UUID auctionId) {
        AuctionResponseDTO response = biddingService.getAuctionStatus(auctionId);
        return ResponseEntity.ok(response);
    }

    // melihat riwayat penawaran dengan paginasi
    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<Page<BidResponseDTO>> getBidHistory(
            @PathVariable UUID auctionId,
            Pageable pageable
    ) {
        Page<BidResponseDTO> response = biddingService.getBidHistory(auctionId, pageable);
        return ResponseEntity.ok(response);
    }

    // melihat hasil akhir lelang (menang atau tidak terjual)
    @GetMapping("/{auctionId}/result")
    public ResponseEntity<AuctionResultDTO> getAuctionResult(@PathVariable UUID auctionId) {
        AuctionResultDTO response = biddingService.getAuctionResult(auctionId);
        return ResponseEntity.ok(response);
    }

    // melihat semua riwayat penawaran milik user yang sedang login
    @GetMapping("/my-bids")
    public ResponseEntity<Page<BidResponseDTO>> getMyBids(
            @RequestHeader("X-User-Id") UUID userId,
            Pageable pageable
    ) {
        Page<BidResponseDTO> response = biddingService.getUserBids(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
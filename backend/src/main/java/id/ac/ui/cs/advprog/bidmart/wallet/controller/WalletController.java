package id.ac.ui.cs.advprog.bidmart.wallet.controller;

import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.HoldResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.TopUpRequest;
import id.ac.ui.cs.advprog.bidmart.wallet.dto.WalletResponse;
import id.ac.ui.cs.advprog.bidmart.wallet.service.WalletService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallets")
@CrossOrigin(origins = "*")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable UUID userId) {
        return ResponseEntity.ok(walletService.getWallet(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getWalletForCurrentUser(Principal principal) {
        return ResponseEntity.ok(walletService.getWallet(resolveUserId(principal)));
    }

    @PostMapping("/{userId}/top-up")
    public ResponseEntity<WalletResponse> topUp(@PathVariable UUID userId,
                                                @Valid @RequestBody TopUpRequest request) {
        return ResponseEntity.ok(walletService.topUp(userId, request));
    }

    @PostMapping("/me/top-up")
    public ResponseEntity<WalletResponse> topUpCurrentUser(
            Principal principal,
            @Valid @RequestBody TopUpRequest request
    ) {
        return ResponseEntity.ok(walletService.topUp(resolveUserId(principal), request));
    }

    @PostMapping("/{userId}/reset")
    public ResponseEntity<WalletResponse> reset(@PathVariable UUID userId) {
        return ResponseEntity.ok(walletService.resetWallet(userId));
    }

    @PostMapping("/me/reset")
    public ResponseEntity<WalletResponse> resetCurrentUser(Principal principal) {
        return ResponseEntity.ok(walletService.resetWallet(resolveUserId(principal)));
    }

    @PostMapping("/{userId}/holds")
    public ResponseEntity<HoldResponse> createHold(@PathVariable UUID userId,
                                                   @Valid @RequestBody HoldRequest request) {
        if (request.getUserId() == null) {
            request.setUserId(userId);
        } else if (!request.getUserId().equals(userId)) {
            throw new IllegalArgumentException("User ID mismatch between path and payload.");
        }
        HoldResponse response = walletService.createHold(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/me/holds")
    public ResponseEntity<HoldResponse> createHoldCurrentUser(
            Principal principal,
            @Valid @RequestBody HoldRequest request
    ) {
        UUID userId = resolveUserId(principal);
        request.setUserId(userId);
        HoldResponse response = walletService.createHold(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/holds/{holdId}/release")
    public ResponseEntity<HoldResponse> releaseHold(@PathVariable UUID holdId) {
        return ResponseEntity.ok(walletService.releaseHold(holdId));
    }

    @PostMapping("/holds/{holdId}/capture")
    public ResponseEntity<HoldResponse> captureHold(@PathVariable UUID holdId) {
        return ResponseEntity.ok(walletService.captureHold(holdId));
    }

    private UUID resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new IllegalArgumentException("User is not authenticated.");
        }
        String subject = principal.getName().trim();
        String source = "wallet-user-" + subject;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }
}

package id.ac.ui.cs.advprog.bidmart.notifications.controller;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID userId = getUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(notificationService.getNotifications(userId, read, page, size));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            Authentication authentication,
            @PathVariable UUID notificationId
    ) {
        UUID userId = getUserIdFromAuthentication(authentication);
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        UUID userId = getUserIdFromAuthentication(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private UUID getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        return UUID.fromString((String) principal.get("userId"));
    }
}
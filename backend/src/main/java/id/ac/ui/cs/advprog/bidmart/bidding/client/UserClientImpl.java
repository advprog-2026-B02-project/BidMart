package id.ac.ui.cs.advprog.bidmart.bidding.client;

import id.ac.ui.cs.advprog.bidmart.backend.auth.service.AuthService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserClientImpl implements UserClient {

    private final AuthService authService;

    public UserClientImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void validateUser(UUID userId) {
        authService.validateUser(userId);
    }
}
package id.ac.ui.cs.advprog.bidmart.bidding.client;

import java.util.UUID;

public interface UserClient {
    // fungsi buat ngecek apakah UUID user ini beneran ada di database User
    void validateUser(UUID userId);
}

package id.ac.ui.cs.advprog.bidmart.bidding.client;

import java.util.UUID;

public interface CatalogClient {
    // fungsi buat ngecek apakah barangnya valid dan bisa dilelang
    void validateListing(UUID listingId);

    // get seller id dari listing
    UUID getSellerId(UUID listingId);
}
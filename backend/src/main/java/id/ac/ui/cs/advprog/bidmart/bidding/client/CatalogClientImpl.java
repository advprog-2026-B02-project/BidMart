package id.ac.ui.cs.advprog.bidmart.bidding.client;

import id.ac.ui.cs.advprog.bidmart.catalog.service.ListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogClientImpl implements CatalogClient {

    private final ListingService listingService;

    @Override
    public void validateListing(UUID listingId) {
        listingService.validateListingForBid(listingId);
    }

    @Override
    public UUID getSellerId(UUID listingId) {
        return listingService.findDetailById(listingId).getSellerId();
    }
}
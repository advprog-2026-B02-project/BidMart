package id.ac.ui.cs.advprog.bidmart.bidding.repository;

import id.ac.ui.cs.advprog.bidmart.bidding.model.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    Page<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId, Pageable pageable);
    Page<Bid> findByBidderId(UUID bidderId, Pageable pageable);
}
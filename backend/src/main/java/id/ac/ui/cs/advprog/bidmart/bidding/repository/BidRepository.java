package id.ac.ui.cs.advprog.bidmart.bidding.repository;

import id.ac.ui.cs.advprog.bidmart.bidding.model.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    Page<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId, Pageable pageable);
    Page<Bid> findByBidderId(UUID bidderId, Pageable pageable);
    @Query("SELECT DISTINCT b.bidderId FROM Bid b WHERE b.auction.id = :auctionId")
    List<UUID> findDistinctBidderIdsByAuctionId(@Param("auctionId") UUID auctionId);
}
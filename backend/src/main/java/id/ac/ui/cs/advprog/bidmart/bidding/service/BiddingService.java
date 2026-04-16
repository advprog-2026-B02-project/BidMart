package id.ac.ui.cs.advprog.bidmart.bidding.service;

import id.ac.ui.cs.advprog.bidmart.bidding.dto.AuctionResponseDTO;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.AuctionResultDTO;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.AuctionStartRequestDTO;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.BidRequestDTO;
import id.ac.ui.cs.advprog.bidmart.bidding.dto.BidResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BiddingService {
    BidResponseDTO placeBid(UUID auctionId, UUID bidderId, BidRequestDTO requestDTO);
    AuctionResponseDTO startAuction(UUID listingId, AuctionStartRequestDTO request);
    AuctionResponseDTO getAuctionStatus(UUID auctionId);
    Page<BidResponseDTO> getBidHistory(UUID auctionId, Pageable pageable);
    AuctionResultDTO getAuctionResult(UUID auctionId);
    void closeExpiredAuctions();
    Page<BidResponseDTO> getUserBids(UUID userId, Pageable pageable);
}
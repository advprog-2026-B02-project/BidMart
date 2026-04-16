package id.ac.ui.cs.advprog.bidmart.bidding.repository;

import id.ac.ui.cs.advprog.bidmart.bidding.model.Auction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    // mengunci data secara pesimistis untuk mencegah anomali saat konkurensi
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdWithPessimisticLock(@Param("id") UUID id);

    // mencari lelang yang waktunya sudah lewat namun statusnya belum ditutup
    @Query("SELECT a FROM Auction a WHERE a.status IN ('ACTIVE', 'EXTENDED') AND a.endTime <= :now")
    List<Auction> findExpiredActiveAuctions(@Param("now") LocalDateTime now);
}
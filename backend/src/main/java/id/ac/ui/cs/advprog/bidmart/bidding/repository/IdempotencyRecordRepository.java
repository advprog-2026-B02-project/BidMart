package id.ac.ui.cs.advprog.bidmart.bidding.repository;

import id.ac.ui.cs.advprog.bidmart.bidding.model.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {
}
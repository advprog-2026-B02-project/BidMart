package id.ac.ui.cs.advprog.bidmart.backend.auth.repository;

import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.PartialAuthSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartialAuthSessionRepository extends JpaRepository<PartialAuthSession, UUID> {
    Optional<PartialAuthSession> findByPartialToken(String partialToken);
}

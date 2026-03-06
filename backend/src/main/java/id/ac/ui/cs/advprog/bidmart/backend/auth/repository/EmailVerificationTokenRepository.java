package id.ac.ui.cs.advprog.bidmart.backend.auth.repository;

import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
}
package id.ac.ui.cs.advprog.bidmart.backend.repository;

import id.ac.ui.cs.advprog.bidmart.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
}
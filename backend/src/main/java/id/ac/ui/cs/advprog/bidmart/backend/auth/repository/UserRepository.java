package id.ac.ui.cs.advprog.bidmart.backend.auth.repository;

import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.User;
import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("select u from User u where (:search is null or lower(u.email) like lower(concat('%', :search, '%')) or lower(u.displayName) like lower(concat('%', :search, '%'))) and (:status is null or u.status = :status)")
    List<User> searchUsers(String search, UserStatus status);
}
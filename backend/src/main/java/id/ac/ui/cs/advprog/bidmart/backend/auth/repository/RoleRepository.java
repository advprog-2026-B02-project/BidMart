package id.ac.ui.cs.advprog.bidmart.backend.auth.repository;

import id.ac.ui.cs.advprog.bidmart.backend.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByNameIgnoreCase(String name);
}

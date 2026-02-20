package id.ac.ui.cs.advprog.bidmart.backend.repository;

import id.ac.ui.cs.advprog.bidmart.backend.model.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CounterRepository extends JpaRepository<PageView, String> {
}
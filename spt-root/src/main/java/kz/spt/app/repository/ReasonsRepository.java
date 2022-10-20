package kz.spt.app.repository;

import kz.spt.lib.model.Reasons;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReasonsRepository extends JpaRepository<Reasons, Long> {
}

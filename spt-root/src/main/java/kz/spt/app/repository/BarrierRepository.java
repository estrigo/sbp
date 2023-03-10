package kz.spt.app.repository;

import kz.spt.lib.model.Barrier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BarrierRepository extends JpaRepository<Barrier, Long> {

}

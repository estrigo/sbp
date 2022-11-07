package kz.spt.app.repository;

import kz.spt.lib.model.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist,Long>, JpaSpecificationExecutor<Blacklist> {
    @Query("select t from Blacklist t" +
            " where t.plateNumber=:plateNumber")
    Optional<Blacklist> findByPlateNumber(@Param("plateNumber") String plateNumber);
}

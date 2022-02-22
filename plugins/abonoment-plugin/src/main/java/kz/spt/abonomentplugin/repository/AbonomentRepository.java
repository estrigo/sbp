package kz.spt.abonomentplugin.repository;

import kz.spt.abonomentplugin.model.Abonoment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AbonomentRepository extends JpaRepository<Abonoment, Long>, JpaSpecificationExecutor<Abonoment> {

    Abonoment findAbonomentByCarPlatenumber(String plateNumber);

    @Query("select a from Abonoment a join fetch a.parking where a.car.platenumber = :plateNumber and a.paid = false and a.begin > :date")
    List<Abonoment> findNotPaidAbonoment(@Param("plateNumber") String plateNumber, @Param("date") Date date, Pageable page);

    @Query("select a from Abonoment a join fetch a.parking where a.car.platenumber = :plateNumber and a.parking.id = :parkingId and a.paid = true and :date between a.begin and a.end")
    List<Abonoment> findPaidNotExpiredAbonoment(@Param("plateNumber") String plateNumber, @Param("parkingId") Long parkingId, @Param("date") Date date, Pageable page);
}
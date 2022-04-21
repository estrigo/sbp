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

    List<Abonoment> findAbonomentByCarPlatenumber(String plateNumber);

    @Query("select a from Abonoment a join fetch a.parking where a.car.platenumber = :plateNumber and a.paid = false and a.end > :date")
    List<Abonoment> findNotPaidAbonoment(@Param("plateNumber") String plateNumber, @Param("date") Date date, Pageable page);

    @Query("select a from Abonoment a join fetch a.parking where a.car.platenumber = :plateNumber and a.parking.id = :parkingId and a.paid = true and ((:carInDate >= a.begin and :carOutDate <= a.end) or (:carInDate <= a.begin and :carOutDate >= a.end) or (:carInDate >= a.begin and :carInDate <= a.end) or (:carOutDate >= a.begin and :carOutDate <= a.end)) order by a.begin asc")
    List<Abonoment> findPaidNotExpiredAbonoment(@Param("plateNumber") String plateNumber, @Param("parkingId") Long parkingId, @Param("carInDate") Date carInDate, @Param("carOutDate") Date carOutDate);

    @Query("select count(a.id) from Abonoment a where a.car.platenumber = :plateNumber and a.parking.id = :parkingId and not ((:begin < a.begin and :end <= a.begin) or (:begin>= a.end and :end > a.end))")
    Long findIntersectionAbonoment(@Param("plateNumber") String plateNumber, @Param("parkingId") Long parkingId, @Param("begin") Date begin, @Param("end") Date end);

    @Query("from Abonoment w where w.parking.id = :parkingId")
    List<Abonoment> findAbonomentByParking(@Param("parkingId") Long parkingId);

}
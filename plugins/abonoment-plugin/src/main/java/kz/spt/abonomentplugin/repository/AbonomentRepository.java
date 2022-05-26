package kz.spt.abonomentplugin.repository;

import kz.spt.abonomentplugin.model.Abonement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface AbonomentRepository extends JpaRepository<Abonement, Long>, JpaSpecificationExecutor<Abonement> {

    List<Abonement> findAbonomentByCarPlatenumber(String plateNumber);

    @Query("select a from Abonement a join fetch a.parking where a.car.platenumber = :plateNumber and a.paid = false and a.end > :date")
    List<Abonement> findNotPaidAbonoment(@Param("plateNumber") String plateNumber, @Param("date") Date date, Pageable page);

    @Query("select a from Abonement a join fetch a.parking where a.car.platenumber = :plateNumber and a.parking.id = :parkingId and a.paid = true and ((:carInDate >= a.begin and :carOutDate <= a.end) or (:carInDate <= a.begin and :carOutDate >= a.end) or (:carInDate >= a.begin and :carInDate <= a.end) or (:carOutDate >= a.begin and :carOutDate <= a.end)) order by a.begin asc")
    List<Abonement> findPaidNotExpiredAbonoment(@Param("plateNumber") String plateNumber, @Param("parkingId") Long parkingId, @Param("carInDate") Date carInDate, @Param("carOutDate") Date carOutDate);

    @Query("select count(a.id) from Abonement a where a.car.platenumber = :plateNumber and a.parking.id = :parkingId and not ((:begin < a.begin and :end <= a.begin) or (:begin>= a.end and :end > a.end))")
    Long findIntersectionAbonoment(@Param("plateNumber") String plateNumber, @Param("parkingId") Long parkingId, @Param("begin") Date begin, @Param("end") Date end);

    @Query("from Abonement w where w.parking.id = :parkingId")
    List<Abonement> findAbonomentByParking(@Param("parkingId") Long parkingId);

    @Query("from Abonement w where w.created < :checkDate and w.paid = false")
    List<Abonement> findExpiredNotPaid(@Param("checkDate") Date checkDate);

    @Query("from Abonement w where w.end between :currentDate and :checkDate and w.paid = true and w.checked = true")
    List<Abonement> findPaidExpiresInFewDays(@Param("currentDate") Date currentDate, @Param("checkDate") Date checkDate);

    @Query("from Abonement w where w.car.platenumber = :platenumber and w.parking.id = :parkingId and w.end between :currentDate and :checkDate and w.paid = true")
    List<Abonement> findExpiresInFewDays(@Param("platenumber") String platenumber, @Param("parkingId") Long parkingId, @Param("currentDate") Date currentDate, @Param("checkDate") Date checkDate);
}
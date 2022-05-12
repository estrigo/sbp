package kz.spt.app.repository;

import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.Parking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


@Repository
public interface CarStateRepository extends JpaRepository<CarState, Long>, JpaSpecificationExecutor<CarState>{

    @Query("from CarState cs LEFT JOIN FETCH cs.parking where cs.carNumber = :carNumber and cs.outTimestamp is null order by cs.inTimestamp desc")
    List<CarState> getCarStateNotLeft(@Param("carNumber") String carNumber, Pageable page);

    @Query("from CarState cs where cs.outTimestamp is null order by cs.inTimestamp desc")
    Iterable<CarState> getAllCarStateNotLeft();

    @Query("from CarState cs where cs.carNumber = :carNumber")
    List<CarState> getAllByPlateNumber(@Param("carNumber") String carNumber);

    @Query("select distinct cs.carNumber from CarState cs where cs.carNumber in (?1) and cs.outTimestamp is null and (cs.paid is null or cs.paid = false)")
    List<String> getInButNotPaidFromList(List<String> checkList);

    @Query("from CarState cs where cs.outChannelIp = :cameraIp order by cs.outTimestamp desc")
    List<CarState> getCarStateLastLeft(@Param("cameraIp") String cameraIp, Pageable page);

    @Query("from CarState cs where cs.carNumber like :carNumber and (cs.paymentId is null or cs.paymentId=0) and cs.inTimestamp >= :date order by cs.inTimestamp ")
    List<CarState> getCurrentNotPayed(@Param("carNumber") String carNumber, @Param("date") Date date);

    @Query("from CarState cs where cs.outChannelIp <> :cameraIp and cs.carNumber = :carNumber and cs.outTimestamp > :secondsBefore order by cs.outTimestamp desc")
    List<CarState> getCarStateLastLeftFromOther(@Param("cameraIp") String cameraIp, @Param("carNumber") String carNumber, @Param("secondsBefore") Date secondsBefore, Pageable page);

    @Query("from CarState cs where cs.inChannelIp <> :cameraIp and cs.carNumber = :carNumber and cs.inTimestamp > :secondsBefore and cs.outTimestamp is null order by cs.inTimestamp desc")
    List<CarState> getCarStateLastEnterFromOther(@Param("cameraIp") String cameraIp, @Param("carNumber") String carNumber, @Param("secondsBefore") Date secondsBefore, Pageable page);

    @Query("from CarState cs where cs.outChannelIp = :cameraIp and cs.carNumber = :carNumber and cs.outTimestamp > :secondsBefore order by cs.outTimestamp desc")
    List<CarState> getCarStateLastLeftFromThis(@Param("cameraIp") String cameraIp, @Param("carNumber") String carNumber, Date secondsBefore, Pageable page);

    @Query("from CarState cs where cs.carNumber = :carNumber order by cs.inTimestamp desc")
    List<CarState> getLastCarState(@Param("carNumber") String carNumber, Pageable page);

    @Transactional
    @Modifying
    @Query(value = "update car_state cs set cs.out_barrier=null, cs.in_barrier=null, cs.in_gate=null, cs.out_gate=null," +
            " cs.parking=null where cs.parking = :parkingId", nativeQuery = true)
    void updateCarStateByParkingId(@Param("parkingId") Long parkingId);

    @Transactional
    @Modifying
    @Query(value = "update car_state set out_barrier = null where out_barrier = :barrierId", nativeQuery = true)
    void updateCarStateByOutBarrier(@Param("barrierId") Long barrierId);

    @Transactional
    @Modifying
    @Query(value = "update car_state set in_barrier = null where in_barrier = :barrierId", nativeQuery = true)
    void updateCarStateByInBarrier(@Param("barrierId") Long barrierId);
}

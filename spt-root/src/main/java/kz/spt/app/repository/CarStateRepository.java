package kz.spt.app.repository;

import kz.spt.lib.model.CarState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    @Query("from CarState cs where cs.outChannelIp <> :cameraIp and cs.carNumber = :carNumber and cs.outTimestamp > :secondsBefore order by cs.outTimestamp desc")
    List<CarState> getCarStateLastLeftFromOther(@Param("cameraIp") String cameraIp, @Param("carNumber") String carNumber, @Param("secondsBefore") Date secondsBefore, Pageable page);

    @Query("from CarState cs where cs.inChannelIp <> :cameraIp and cs.carNumber = :carNumber and cs.inTimestamp > :secondsBefore and cs.outTimestamp is null order by cs.inTimestamp desc")
    List<CarState> getCarStateLastEnterFromOther(@Param("cameraIp") String cameraIp, @Param("carNumber") String carNumber, @Param("secondsBefore") Date secondsBefore, Pageable page);
}

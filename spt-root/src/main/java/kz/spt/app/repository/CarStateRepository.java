package kz.spt.app.repository;

import kz.spt.lib.model.CarState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CarStateRepository extends JpaRepository<CarState, Long>, JpaSpecificationExecutor<CarState>{

    @Query("from CarState cs where cs.carNumber = :carNumber and cs.outTimestamp is null order by cs.inTimestamp desc")
    CarState getCarStateNotLeft(@Param("carNumber") String carNumber);

    @Query("from CarState cs where cs.outTimestamp is null order by cs.inTimestamp desc")
    Iterable<CarState> getAllCarStateNotLeft();

    @Query("from CarState cs where cs.carNumber = :carNumber")
    List<CarState> getAllByPlateNumber(@Param("carNumber") String carNumber);
}

package kz.spt.app.repository;

import kz.spt.api.model.CarState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CarStateRepository extends JpaRepository<CarState, Long> {

    @Query("from CarState cs where cs.carNumber = :carNumber and cs.outTimestamp is null order by cs.inTimestamp desc")
    CarState getCarStateNotLeft(@Param("carNumber") String carNumber);
}

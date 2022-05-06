package kz.spt.megaplugin.repository;

import kz.spt.megaplugin.model.ThirdPartyCars;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository("thirdPartyCars")
public interface ThirdPartyCarsRepository extends JpaRepository<ThirdPartyCars, Long> {

    @Query("from ThirdPartyCars t where t.car_number = ?1")
    ThirdPartyCars findByPlateNumber(String plateNumber);

}

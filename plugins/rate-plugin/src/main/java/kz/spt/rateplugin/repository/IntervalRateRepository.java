package kz.spt.rateplugin.repository;

import kz.spt.rateplugin.model.IntervalRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntervalRateRepository extends JpaRepository<IntervalRate, Long> {

    List<IntervalRate> findAllByParkingRateId(Long parkingRateId);

    List<IntervalRate> findAllByDimensionSetId(Long dimensionsId);
}

package kz.spt.rateplugin.repository;

import kz.spt.rateplugin.model.RateCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateConditionRepository extends JpaRepository<RateCondition, Long> {

    List<RateCondition> findAllByIntervalRateId(Long id);
}

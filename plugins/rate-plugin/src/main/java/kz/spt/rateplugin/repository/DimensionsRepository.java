package kz.spt.rateplugin.repository;

import kz.spt.lib.model.Dimensions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DimensionsRepository extends JpaRepository<Dimensions, Long> {
    List<Dimensions> findAll();
    Dimensions getOne(Long aLong);
}

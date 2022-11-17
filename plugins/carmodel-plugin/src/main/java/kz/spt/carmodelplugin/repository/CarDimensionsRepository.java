package kz.spt.carmodelplugin.repository;

import kz.spt.lib.model.Dimensions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarDimensionsRepository extends JpaRepository<Dimensions, Long> {
    List<Dimensions> findAll();

    @Query(nativeQuery = true, value = "SELECT * FROM dimensions c WHERE c.id = :id")
    Dimensions getById(Long id);
}

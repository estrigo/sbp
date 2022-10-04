package kz.spt.abonomentplugin.repository;

import kz.spt.lib.model.Cars;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarsRepository extends JpaRepository<Cars, Long> {
}

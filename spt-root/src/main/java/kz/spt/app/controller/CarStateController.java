package kz.spt.app.controller;

import kz.spt.lib.model.CarState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarStateController extends JpaRepository<CarState, Long> {

}

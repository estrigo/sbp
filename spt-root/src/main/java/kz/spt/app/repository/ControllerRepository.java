package kz.spt.app.repository;

import kz.spt.lib.model.Controller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ControllerRepository extends JpaRepository<Controller, Long> {

}

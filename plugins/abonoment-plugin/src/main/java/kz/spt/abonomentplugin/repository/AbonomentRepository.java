package kz.spt.abonomentplugin.repository;

import kz.spt.abonomentplugin.model.Abonoment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbonomentRepository extends JpaRepository<Abonoment, Long>, JpaSpecificationExecutor<Abonoment> {

    Abonoment findAbonomentByCarPlatenumber(String plateNumber);
}

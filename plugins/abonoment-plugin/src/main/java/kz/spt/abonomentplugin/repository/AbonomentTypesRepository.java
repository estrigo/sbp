package kz.spt.abonomentplugin.repository;

import kz.spt.abonomentplugin.model.AbonomentTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AbonomentTypesRepository  extends JpaRepository<AbonomentTypes, Long>, JpaSpecificationExecutor<AbonomentTypes> {

}

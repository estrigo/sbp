package kz.spt.app.repository;

import kz.spt.lib.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PropertyRepository extends JpaRepository<Property, Integer> {

    Optional<Property> findFirstByKey(String key);
}

package crm.repository;

import crm.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Role findByName(String name);

    @Query("from Role r where r.plugin is null or r.plugin in :plugins")
    List<Role> findAllByPluginIn(@Param("plugins") List<String> plugins);

    List<Role> findAllByPluginIsNull();
}

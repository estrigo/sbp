package kz.spt.app.repository;

import kz.spt.lib.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    Iterable<User> findAllByEnabled (int enabled);

    @Query(value = "select password from users u where u.id = ?1", nativeQuery = true)
    String getPasswordHashFromDb(Long id);
}

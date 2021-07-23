package kz.spt.app.repository;

import kz.spt.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);

    Iterable<User> findAllByEnabled (int enabled);

}

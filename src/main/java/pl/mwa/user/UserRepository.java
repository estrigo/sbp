package pl.mwa.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pl.mwa.position.Position;
import pl.mwa.role.Role;
@Repository
public interface UserRepository extends JpaRepository<User, Long>{

	Optional<User> findById(Long id);

	User findByUsername(String username);
	
	List<User> findByPosition(Position position);
	
	List<User> findByRolesIn(List<Role> roles);



}

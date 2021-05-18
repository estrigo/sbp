package pl.mwa.team;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>{

	
	Optional<Team> findByLeaderId(Long id); 
	
	List<Team> findAllByNameIgnoreCaseContaining(String name);
	
}

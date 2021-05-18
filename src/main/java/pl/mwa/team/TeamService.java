package pl.mwa.team;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pl.mwa.exception.ModelNotFound;

@Service
public class TeamService {

	@Autowired
	TeamRepository repository;
	
	
	
	Long createTeam(Team team) {
		repository.save(team);
		return team.getId();
	}
	
	
	Team getTeam(Long id) {
		if (id != null) {
			return repository.getOne(id);
		} 
		return null;
	}
	
	List<Team> getTeams() {
		return repository.findAll();
	}
	
	
	void updateTeam(Team team) {
		repository.save(team);
	}
	
	void deleteTeam(Long id) {
		repository.deleteById(id);
	}
	
	List<Team> getTeams(String name) {
		return repository.findAllByNameIgnoreCaseContaining(name);
	}
	
	Team getTeamByLeaderId(Long id) {
		return repository.findByLeaderId(id)
						 .orElseThrow(() -> new ModelNotFound("Leader", id));
	}
	
	
	
}

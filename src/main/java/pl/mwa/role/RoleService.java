package pl.mwa.role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService {

	@Autowired
	RoleRepository repository;
	
	
	public RoleService(RoleRepository repository) {
		this.repository = repository;
	}
	
	
	
	Role getRole(Long id) {
		return repository.getOne(id);
		
	}
	
	public Role findByName(String name) {
		return repository.findOneByName(name);
	}
	
	
}

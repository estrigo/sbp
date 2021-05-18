package pl.mwa.representative;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import pl.mwa.client.Client;
import pl.mwa.client.ClientService;
import pl.mwa.exception.ModelNotFound;
import pl.mwa.util.CSVUtils;
import pl.mwa.util.EntityUtils;

@Service
public class RepresentativeService {

	
	private static final Logger log = LoggerFactory.getLogger(RepresentativeService.class);
	
	@Autowired
	RepresentativeRepository repository;
	
	
	Long createRepresentative(CreateRepresentativeDto dto) {
		return save(RepresentativeMapper.toEntity(dto)).getId();
	}
	
	Representative save(Representative r) {
		r.setActive(true);
		return repository.save(r);
	}
	
	

	RepresentativeDto findByClient(Client client) {
		return jsonNullReference(repository.findByClient(client)
				.map(RepresentativeMapper::toDto)
				.orElseThrow(() -> new ModelNotFound("Client", client.getId())));
	}

	public RepresentativeDto findOne(long id) {
		return jsonNullReference(repository.findById(id)
				.map(RepresentativeMapper::toDto)
				.orElseThrow(()-> new ModelNotFound("Representative", id)));
			
	}	

	
	public void remove(long id) {
		Representative rep = repository.findOne(id);
		rep.setActive(false);
		repository.save(rep);
	}

	
	public void update(RepresentativeDto dto) {
		Representative r = repository.findById(dto.getId())
				.orElseThrow(()-> new ModelNotFound("Representative", dto.getId()));
		EntityUtils.setter(dto.getFirstname(), t->r.setFirstname(t));
		EntityUtils.setter(dto.getLastname(), t->r.setLastname(t));
		EntityUtils.setter(dto.getEmail(), t->r.setEmail(t));
		EntityUtils.setter(dto.getPhone(), t->r.setPhone(t));
		EntityUtils.setter(dto.getPosition(), t->r.setPosition(t));
		EntityUtils.setter(dto.getClient(), t->r.setClient(t));
		repository.save(r);
	}


	public List<RepresentativeDto> findAllByActiveTrue() {
		return jsonListNullReference(repository.findAllByActiveTrue()
				.stream()
				.map(RepresentativeMapper::toDto)
				.collect(Collectors.toList()));
	}



	public Page<Representative> findAll(Pageable pageable) {
		return repository.findAllByActiveTrue(pageable);
	}





	
	RepresentativeDto jsonNullReference(RepresentativeDto representative) {
		if (representative != null) {
			if (representative.getClient() != null) {
				representative.getClient().setRepresentatives(null);
				representative.getClient().setResponsible(null);
				if (representative.getClient().getAddress() != null) {
					representative.getClient().getAddress().setClient(null);
				}
			}
		}
		return representative;
	}
	
	List<RepresentativeDto> jsonListNullReference(List<RepresentativeDto> list){
		for (RepresentativeDto dto : list) {
			dto = jsonNullReference(dto);
		}
		return list;
	}
	
	public void saveToDB(List<Representative> list) {
		repository.save(list);
	}
	
	
	public void importDataFromCSV(String filename) {
		List<Representative> representatives = CSVUtils.buildListFromCSV(filename, Representative.class);
		saveToDB(representatives);
	}
	
	
	public void exportDataToCSV(String filename) {
		List<Representative> representatives = buildListFromDB();
		CSVUtils.exportListToCSV(filename, representatives);
	}
	
	public List<Representative> buildListFromDB() {
		return repository.findAll();
	}



	public Collection<Representative> findAll() {
		return repository.findAll();
	}
	
	
}
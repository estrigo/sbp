package pl.mwa.address;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pl.mwa.client.Client;
import pl.mwa.util.CSVUtils;




@RestController
@RequestMapping("/addresses")
public class AddressResource {

	@Autowired
	AddressService service;
	
	
	private static final Logger log = LoggerFactory.getLogger(AddressResource.class);
	
	@GetMapping("/{id}")
	ResponseEntity getAddress(@PathVariable("id") Long id ) {
		return ResponseEntity.ok(service.getAddress(id));
	}

	@GetMapping
	ResponseEntity getAddresses() {
		return ResponseEntity.ok(service.getAddresses());
	}
	
	@GetMapping("/search")
	ResponseEntity getSearchRequestAddresses(@RequestParam(name="street", required=false) String street, 
			@RequestParam(name="streetNumber", required=false) String streetNumber,
			@RequestParam(name="additionalNumber", required=false) String additionalNumber,
			@RequestParam(name="postalCode", required=false) String postalCode, 
			@RequestParam(name="city", required=false) String city, 
			@RequestParam(name="country", required=false) String country, 
			@RequestParam(name="client", required=false) Client client) {
		return ResponseEntity.ok(service.getAddressesSearch(street, 
				streetNumber, additionalNumber, postalCode, city, country, client));
	}
	
	@PostMapping
	ResponseEntity createAddress(@RequestBody @Valid CreateAddressDto dto) {
		long id = service.createAddress(dto);
		log.info("Address with id: "+id+" created");
		return ResponseEntity.ok(id);
	}
	
	@PutMapping
	ResponseEntity updateAddress(@RequestBody @Valid AddressDto dto) {
		service.update(dto);
		log.info("Address with id: "+dto.getId()+" updated");
		return ResponseEntity.accepted().build();
	}
	
	@DeleteMapping("/{id}")
	ResponseEntity deleteAddress(@PathVariable("id") long id) {
		service.deleteAddress(id);
		log.info("Address with id: "+id+" deleted");
		return ResponseEntity.accepted().build();
	}
	
	@GetMapping("/export")
	ResponseEntity exportTableToDB(@RequestParam(name = "filename", required = true) String filename) {
		service.exportDataToCSV(filename);
		return ResponseEntity.accepted().build();
	}
	
	@GetMapping("/import")
	ResponseEntity getImportListFromCSV(@RequestParam(name = "filename", required = true) String filename) {
		return ResponseEntity.ok(CSVUtils.buildListFromCSV(filename, Address.class));
	}
	
	
	
}

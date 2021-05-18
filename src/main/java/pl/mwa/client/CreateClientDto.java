package pl.mwa.client;

import java.util.Collection;

import javax.persistence.Column;

import org.hibernate.validator.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.mwa.address.Address;
import pl.mwa.representative.Representative;
import pl.mwa.user.User;






@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientDto {

	@NotBlank
	@Column(nullable = false, unique = true)
	private String name;

	private String abbr;
	
	private Boolean active;
	
	private User responsible;
	
	private Collection<Representative> representatives;
	
	private Address address;
	
	private Industry industry;
	
}

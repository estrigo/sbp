package pl.mwa.address;

import javax.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.mwa.client.Client;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {

	private Long id;
	
	private String street;
	
	private String streetNumber;

	private String additionalNumber;

	@Pattern(regexp="\\d{2}-\\d{3}")
	private String postalCode;
	
	private String city;

	private String country;
	
	private Client client;
	
	
}

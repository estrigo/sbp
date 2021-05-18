package pl.mwa.address;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.mwa.client.Client;



@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "addresses")
public class Address {

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String street;
	
	private String streetNumber;

	private String additionalNumber;

	@Pattern(regexp="\\d{2}-\\d{3}")
	private String postalCode;
	
	@NotBlank
	@Column(nullable = false)
	private String city;
	
	@Column
	private String country;
	
	@OneToOne(mappedBy = "address", cascade = CascadeType.ALL,  fetch = FetchType.LAZY)
	private Client client;
	
	
	
}

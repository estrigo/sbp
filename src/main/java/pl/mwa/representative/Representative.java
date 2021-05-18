package pl.mwa.representative;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonManagedReference;

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
@Table(name="representatives")
public class Representative {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	private String firstname;
	
	@NotBlank
	private String lastname;
	
	private String position;

	@Pattern(regexp="\\d{3}-\\d{3}-\\d{3}|\\d{2}-\\d{3}-\\d{2}-\\d{2}") // tel. pattern XXX-XXX-XXX or XX-XXX-XX-XX
	private String phone;
	
	@Email
	@Column(nullable = false, unique = true)
	private String email;
	
	private boolean active;
	
	
	
	@ManyToOne
	private Client client;
	
	
	public String getFullname() {
		return firstname + " " + lastname;
	}
	
	
}

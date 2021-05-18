package pl.mwa.user;

import java.sql.Timestamp;
import java.util.Set;

import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.mwa.position.Position;
import pl.mwa.role.Role;
import pl.mwa.team.Team;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDto {

	@NotBlank
	private String username;
	
	@NotBlank
	private String password;
	
	@NotBlank
	private String firstname;
	
	@NotBlank
	private String lastname;
	
	@NotBlank
	@Email
	private String email;
	
	@Pattern(regexp="\\d{3}-\\d{3}-\\d{3}|\\d{2}-\\d{3}-\\d{2}-\\d{2}") // tel. pattern XXX-XXX-XXX or XX-XXX-XX-XX
	private String phone;
	
	private Boolean active;
	
	private Timestamp created;
	
	private Set<Role> roles;
	
	private Position position;
	
	private Team leading;
	
}

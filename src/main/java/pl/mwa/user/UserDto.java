package pl.mwa.user;

import java.sql.Timestamp;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Email;

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
public class UserDto {

	@NotNull
	private Long id;
	

	private String username;
	

	private String password;
	

	private String firstname;
	

	private String lastname;
	

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

package pl.mwa.workflow;

import java.sql.Timestamp;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.validator.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.mwa.client.Client;
import pl.mwa.document.Document;
import pl.mwa.representative.Representative;
import pl.mwa.user.User;

/**
 * 
 * @author mikolaj
 *	
 *	Class Workflow represents workflow of documents
 *	 
 *
 *
 *
 */


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="workflows")
public class Workflow {

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@NotBlank
	private String title;
	
	private String description;
	
	private Timestamp created;

	@ManyToOne
	@JoinColumn(name="user_id")
	private User user;
	
	@ManyToOne
	@JoinColumn(name="client_id")
	private Client client;
	
	@ManyToOne
	@JoinColumn(name="representative_id")
	private Representative representative; 
	
	@OneToMany
	private List<Document> documents;
	
	
	
	
	
	
}

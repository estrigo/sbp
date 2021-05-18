package pl.mwa.document;

import java.sql.Timestamp;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.mwa.client.Client;
import pl.mwa.user.User;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="documents")
public class Document {

	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY )
	private long id;
	
	@NotBlank
	@Column(nullable = false, unique = true )
	private String title;
	
	
	@NotNull
	@Column(nullable = false)
	private Timestamp created;
	
	@Enumerated(EnumType.STRING)
	private DocumentType documentType;
	
	@Enumerated(EnumType.STRING)
	private DocumentStatus documentStatus;
	
	@ManyToOne
	private Client client;
	
	private String description;
	
	private String filename;
	
	@Column(precision=10, scale=2)
	private Double value;
	
	@ManyToOne
	@JoinColumn(name = "user_id")
	private User author;
	
	@ManyToOne
	private User acceptedBy;
	
	private Boolean acceptedItern;
	
	private Boolean acceptedByClient;
	
	
}

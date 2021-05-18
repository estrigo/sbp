package pl.mwa.document;

import java.sql.Timestamp;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

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
public class DocumentDto {

	@NotNull
	private long id;

	private String title;

	private Timestamp created;
	
	@Enumerated(EnumType.STRING)
	private DocumentType documentType;
	
	@Enumerated(EnumType.STRING)
	private DocumentStatus documentStatus;
	
	private Client client;
	
	private String description;
	
	private Double value;
	
	private User author;
	
	private User acceptedBy;
	
	private Boolean acceptedItern;
	
	private Boolean acceptedByClient;


	
	
	
}

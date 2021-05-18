package pl.mwa.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long>{

	Optional<Document> findById(long id);

	List<Document> findAllByDocumentType(DocumentType documentType);

	List<Document> findAllByDocumentStatus(DocumentStatus documentStatus);

	List<Document> findAllByDocumentTypeAndDocumentStatus(DocumentType documentType,DocumentStatus documentStatus);
	
}

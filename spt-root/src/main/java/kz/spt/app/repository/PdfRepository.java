package kz.spt.app.repository;

import kz.spt.app.model.Pdf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PdfRepository extends JpaRepository<Pdf, Long> {

    Pdf findByName(String name);

}

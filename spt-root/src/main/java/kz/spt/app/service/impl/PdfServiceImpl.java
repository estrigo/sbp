package kz.spt.app.service.impl;

import kz.spt.app.model.Pdf;
import kz.spt.app.repository.PdfRepository;
import kz.spt.app.service.PdfService;
import org.springframework.stereotype.Service;

@Service
public class PdfServiceImpl implements PdfService {

    private PdfRepository pdfRepository;

    public PdfServiceImpl(PdfRepository pdfRepository) {
        this.pdfRepository = pdfRepository;
    }

    @Override
    public Pdf findByName(String name) {
        return pdfRepository.findByName(name);
    }

    @Override
    public void savePdf(Pdf pdf) {
        pdfRepository.save(pdf);
    }

}

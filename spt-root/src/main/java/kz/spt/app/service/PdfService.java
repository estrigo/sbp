package kz.spt.app.service;

import kz.spt.app.entity.Pdf;

public interface PdfService {

    Pdf findByName(String name);

    void savePdf(Pdf pdf);

}

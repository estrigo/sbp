package kz.spt.megaplugin.service;

import kz.spt.megaplugin.model.RequestThPP;
import kz.spt.megaplugin.model.ResponseThPP;

import java.math.BigDecimal;
import java.util.Date;

public interface ThirdPartyPaymentService {

    Boolean checkCarIfThirdPartyPayment (String plateNumber);

    void saveThirdPartyPayment (String plateNumber, Date entryDate, Date exitDate, BigDecimal rate);

    ResponseThPP addClient(RequestThPP requestThPP);

    ResponseThPP removeClient(RequestThPP requestThPP);
}

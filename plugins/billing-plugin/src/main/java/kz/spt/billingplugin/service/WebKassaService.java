package kz.spt.billingplugin.service;

import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.dto.OfdCheckData;
import kz.spt.billingplugin.model.dto.webkassa.AuthRequestDTO;
import kz.spt.billingplugin.model.dto.webkassa.Check;
import kz.spt.billingplugin.model.dto.webkassa.CheckResponse;
import kz.spt.billingplugin.model.dto.webkassa.ZReport;

public interface WebKassaService {

    OfdCheckData registerCheck(Object check, PaymentProvider provider);

    String closeOperationDay(ZReport zReport, PaymentProvider provider);




}

package kz.spt.billingplugin.service;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.dto.OfdCheckData;

public interface FiscalCheckService {
    OfdCheckData registerCheck(PaymentProvider provider, JsonNode command);
}

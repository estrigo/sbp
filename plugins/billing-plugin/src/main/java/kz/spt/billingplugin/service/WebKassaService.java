package kz.spt.billingplugin.service;

import kz.spt.billingplugin.model.dto.webkassa.AuthRequestDTO;
import kz.spt.billingplugin.model.dto.webkassa.Check;
import kz.spt.billingplugin.model.dto.webkassa.CheckResponse;
import kz.spt.billingplugin.model.dto.webkassa.ZReport;

public interface WebKassaService {

    CheckResponse registerCheck(Check check, AuthRequestDTO authRequestDTO);

    String closeOperationDay(ZReport zReport, AuthRequestDTO authRequestDTO);




}

package kz.spt.lib.service;

import kz.spt.lib.model.dto.RateQueryDto;
import kz.spt.lib.model.dto.payment.CommandDto;

import java.math.BigDecimal;

public interface PaymentService {

    Object billingInteractions(CommandDto commandDto) throws Exception;

    BigDecimal getRateValue(RateQueryDto rateQueryDto) throws Exception;
}

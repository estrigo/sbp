package kz.spt.lib.service;

import kz.spt.lib.model.dto.payment.CommandDto;

public interface PaymentService {

    Object billingInteractions(CommandDto commandDto) throws Exception;
}

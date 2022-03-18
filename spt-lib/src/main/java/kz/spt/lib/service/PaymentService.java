package kz.spt.lib.service;

import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.RateQueryDto;
import kz.spt.lib.model.dto.parkomat.ParkomatCommandDTO;
import kz.spt.lib.model.dto.payment.CommandDto;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentService {

    Object billingInteractions(CommandDto commandDto) throws Exception;

    /**
     * Обработка команды поступающих с паркомата
     * @param parkomatCommandDto
     * @return
     * @throws Exception
     */
    Object billingInteractions(ParkomatCommandDTO parkomatCommandDto) throws Exception;

    BigDecimal getRateValue(RateQueryDto rateQueryDto) throws Exception;

    void createDebtAndOUTState(String carNumber, Camera camera, Map<String, Object> properties) throws Exception;
}

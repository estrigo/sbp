package kz.spt.billingplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.dto.OfdCheckData;
import kz.spt.billingplugin.model.dto.rekassa.RekassaCheckRequest;
import kz.spt.billingplugin.model.dto.webkassa.AuthRequestDTO;
import kz.spt.billingplugin.model.dto.webkassa.Check;
import kz.spt.billingplugin.model.dto.webkassa.Position;
import kz.spt.billingplugin.repository.PaymentProviderRepository;
import kz.spt.billingplugin.repository.PaymentRepository;
import kz.spt.billingplugin.service.FiscalCheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;

@Slf4j
@Service
@EnableScheduling
@Transactional(noRollbackFor = Exception.class)
public class FiscalCheckServiceImpl implements FiscalCheckService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fiscalization.enabled}")
    private Boolean checkFiscalization;

    @Value("${fiscalization.mobile}")
    Boolean mobilePayFiscalization;

    @Value("${fiscalization.periodHour}")
    Integer periodHour;

    private final WebKassaServiceImpl webKassaService;
    private final PaymentRepository paymentRepository;
    private final PaymentProviderRepository paymentProviderRepository;

    public FiscalCheckServiceImpl(WebKassaServiceImpl webKassaService, PaymentRepository paymentRepository,
                                  PaymentProviderRepository paymentProviderRepository) {
        this.webKassaService = webKassaService;
        this.paymentProviderRepository = paymentProviderRepository;
        this.paymentRepository = paymentRepository;
    }

    @Scheduled(fixedRateString = "${fiscalization.frequency}")
    public void scheduledRequestForFiscalReceipt() {
        if (checkFiscalization) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, -periodHour);
            List<Payment> paymentList;
            if (mobilePayFiscalization) {
                paymentList = paymentRepository.findAllByCreatedAfterAndCheckNumberIsNull(
                                calendar.getTime());
            } else {
                List<PaymentProvider> paymentProviderList = paymentProviderRepository.findAllByIsParkomatIsTrue();
                paymentList =
                        paymentRepository.findAllByCreatedAfterAndProviderInAndCheckNumberIsNull(
                                calendar.getTime(), paymentProviderList);
            }
            log.info("[WebKassa] " + paymentList.size() + " payments with no check number.");
            for (Payment p : paymentList) {
                ObjectNode node = this.objectMapper.createObjectNode();
                node.put("command", "getCheck");
                node.put("parkomatId", p.getProvider().getClientId());
                node.put("sum", p.getPrice());
                node.put("change", 0);
                node.put("txn_id", p.getTransaction());
                node.put("operationName", "Оплата парковки, ГРНЗ: " + p.getCarNumber());
                node.put("paymentType", p.isIkkm() ? 1 : 4);
                OfdCheckData ofdCheckData = registerCheck(p.getProvider(), node);
                log.info("[WebKassa] Response ofdCheckData: " + ofdCheckData.toString());
                if (ofdCheckData.getCheckNumber() != null) {
                    p.setCheckNumber(ofdCheckData.getCheckNumber());
                    p.setCheckUrl(ofdCheckData.getCheckUrl());
                }
            }
            paymentRepository.saveAll(paymentList);
        }
    }

    @Override
    public OfdCheckData registerCheck(PaymentProvider provider, JsonNode command) {
        int sum = command.get("sum").intValue();
        int change = command.get("change").intValue();
        String operationName = command.get("operationName").textValue();
        int paymentType = command.get("paymentType").intValue();
        String txn_id = command.get("txn_id").textValue();
        OfdCheckData ofdCheckData = null;
        if (provider.getOfdProviderType().equals(PaymentProvider.OFD_PROVIDER_TYPE.WebKassa)) {
            String cashboxNumber = provider.getWebKassaID();
            Check check = new Check();
            check.setCashboxUniqueNumber(cashboxNumber);
            Position position = new Position();
            position.price = sum - change;
            position.positionName = operationName;
            check.getPositions().add(position);

            kz.spt.billingplugin.model.dto.webkassa.Payment payment = new kz.spt.billingplugin.model.dto.webkassa.Payment();
            payment.paymentType = paymentType;
            payment.sum = String.valueOf(sum);
            check.getPayments().add(payment);
            check.setChange(String.valueOf(change));
            check.setExternalCheckNumber(txn_id + "-" + provider.getId());
            AuthRequestDTO authRequestDTO = new AuthRequestDTO();
            authRequestDTO.setPassword(provider.getWebKassaPassword());
            authRequestDTO.setLogin(provider.getWebKassaLogin());
            log.info("[WebKassa] Request for check number for txn " + txn_id);
            ofdCheckData = webKassaService.registerCheck(check, provider);
            log.info("[WebKassa] Result " + ofdCheckData.getCheckNumber());
        } else if (provider.getOfdProviderType().equals(PaymentProvider.OFD_PROVIDER_TYPE.ReKassa)) {
            RekassaCheckRequest checkRequest = new RekassaCheckRequest();
            checkRequest.fillPayment(sum, change, paymentType == 1);
            log.info("[ReKassa] Request for check number for txn " + txn_id);
            ofdCheckData = webKassaService.registerCheck(checkRequest, provider);
            log.info("[ReKassa] Result " + ofdCheckData.getCheckNumber());
        }
        return ofdCheckData;
    }
}

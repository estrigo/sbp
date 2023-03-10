package kz.spt.megaplugin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.lib.utils.StaticValues;
import kz.spt.megaplugin.model.RequestThPP;
import kz.spt.megaplugin.model.ResponseThPP;
import kz.spt.megaplugin.model.ThirdPartyCars;
import kz.spt.megaplugin.model.ThirdPartyPayment;
import kz.spt.megaplugin.repository.ThirdPartyCarsRepository;
import kz.spt.megaplugin.repository.ThirdPartyPaymentRepository;
import kz.spt.megaplugin.service.RootServicesGetterService;
import kz.spt.megaplugin.service.ThirdPartyPaymentService;
import lombok.extern.java.Log;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Log
@Service
@EnableScheduling
@Transactional(noRollbackFor = Exception.class)
public class ThirdPartyPaymentServiceImpl implements ThirdPartyPaymentService {

    @Value("${thirdPartyPayment.url}")
    String thirdPartyPaymentUrl;

    private ThirdPartyCarsRepository thirdPartyCarsRepository;
    private ThirdPartyPaymentRepository thirdPartyPaymentRepository;

    public ThirdPartyPaymentServiceImpl(ThirdPartyCarsRepository thirdPartyCarsRepository,
                                        ThirdPartyPaymentRepository thirdPartyPaymentRepository) {
        this.thirdPartyCarsRepository = thirdPartyCarsRepository;
        this.thirdPartyPaymentRepository = thirdPartyPaymentRepository;
    }

    SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);

    public Boolean checkCarIfThirdPartyPayment(String plateNumber) {
        ThirdPartyCars thirdPartyCars = thirdPartyCarsRepository.findByPlateNumber(plateNumber);
        if (thirdPartyCars != null && thirdPartyCars.getCarNumber() != null
                && thirdPartyCars.getType().equals("direct") && thirdPartyCars.getStatus()) {
            return true;
        } else {
            return false;
        }
    }


    public void saveThirdPartyPayment(String plateNumber, Date entryDate, Date exitDate, BigDecimal rate,
                                      String parkingUid, String thPPUrl) throws Exception {
        Optional<ThirdPartyPayment> existPayment = thirdPartyPaymentRepository.findFirstByCarNumberAndEntryDate(plateNumber, entryDate);
        if (existPayment.isEmpty()) {
            Object statusResp = sendPayment(plateNumber, entryDate, exitDate, rate, parkingUid, thPPUrl);
            log.info("statusResp : " + statusResp);
            ThirdPartyPayment thirdPartyPayment = new ThirdPartyPayment();
            thirdPartyPayment.setCarNumber(plateNumber);
            thirdPartyPayment.setEntryDate(entryDate);
            thirdPartyPayment.setExitDate(exitDate);
            thirdPartyPayment.setRateAmount(rate);
            thirdPartyPayment.setParkingUID(parkingUid);
            if (statusResp != null && statusResp.equals("OK")) {
                thirdPartyPayment.setSent(true);
            } else {
                thirdPartyPayment.setSent(false);
            }
            thirdPartyPaymentRepository.saveAndFlush(thirdPartyPayment);
        }
    }

    @Scheduled(fixedRate = 900000)
    public void resendPayments() throws Exception {
        List<ThirdPartyPayment> thPPList = thirdPartyPaymentRepository.findNotSentThirdPartyPayments();
        if (thPPList != null) {
            for (ThirdPartyPayment pp : thPPList) {
                Object statusResp = sendPayment(pp.getCarNumber(), pp.getEntryDate(), pp.getExitDate(),
                        pp.getRateAmount(), pp.getParkingUID(), thirdPartyPaymentUrl);
                if (statusResp != null && statusResp.equals("OK")) {
                    pp.setSent(true);
                } else {
                    pp.setSent(false);
                }
                thirdPartyPaymentRepository.save(pp);
            }
        }
    }

    private Object sendPayment(String plateNumber, Date entryDate, Date exitDate, BigDecimal rate,
                               String parkingUid, String thPPUrl) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> params = new HashMap<>();
        params.put("plate", plateNumber);
        params.put("sum", rate);
        params.put("in_date", format.format(entryDate));
        params.put("out_date", format.format(exitDate));
        params.put("message", "?????????? ???????????? ???? ?????????????????????????? ????????????");
        params.put("parking_uid", parkingUid);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity request = new HttpEntity<>(params, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(thPPUrl, request, String.class);
            Object status = null;
            if (responseEntity.getBody() != null) {
                JSONObject jsonResp = new JSONObject(responseEntity.getBody());
                status = jsonResp.get("status");
            }
            return status;
        } catch (Exception e) {
//            e.printStackTrace();
            return "error";
        }
    }

    public ResponseThPP removeClient(RequestThPP requestThPP) {
        ThirdPartyCars thirdPartyCars = thirdPartyCarsRepository.findByPlateNumber(requestThPP.getPlatenumber());
        ResponseThPP res = new ResponseThPP();
        if (thirdPartyCars != null && requestThPP.getCommand().equals("delete")) {
            thirdPartyCarsRepository.delete(thirdPartyCars);
            res.setResult(0);
            res.setMessage("?????????????? ??????????????");
        } else if (thirdPartyCars != null && requestThPP.getCommand().equals("freeze")) {
            thirdPartyCars.setStatus(false);
            thirdPartyCarsRepository.save(thirdPartyCars);
            res.setResult(0);
            res.setMessage("?????????????? ??????????????");
        } else {
            res.setResult(1);
            res.setMessage("???????????? ?? ?????????? ?????????????? ???? ????????????????????");
        }
        return res;
    }

    public ResponseThPP addClient(RequestThPP requestThPP) {
        ThirdPartyCars thirdPartyCars = thirdPartyCarsRepository.findByPlateNumber(requestThPP.getPlatenumber());
        ResponseThPP res = new ResponseThPP();
        if (thirdPartyCars != null && requestThPP.getCommand().equals("add") &&
                !requestThPP.getType().equals(thirdPartyCars.getType())) {
            thirdPartyCars.setType(requestThPP.getType());
            thirdPartyCarsRepository.save(thirdPartyCars);
            res.setResult(1);
            res.setMessage("?????????? ?????????????? ?????????????????? ???? ???????????? ?????? ????????????");
        } else if (thirdPartyCars != null && requestThPP.getCommand().equals("add") &&
                requestThPP.getType().equals(thirdPartyCars.getType())) {
            res.setResult(2);
            res.setMessage("???????????? ?? ?????????? ?????????????? ?????? ????????????????????");
        } else if (thirdPartyCars != null && requestThPP.getCommand().equals("restore")) {
            thirdPartyCars.setStatus(true);
            thirdPartyCarsRepository.save(thirdPartyCars);
            res.setResult(0);
            res.setMessage("?????????????? ????????????????");
        } else {
            ThirdPartyCars ss = new ThirdPartyCars();
            ss.setCarNumber(requestThPP.getPlatenumber());
            ss.setType(requestThPP.getType());
            ss.setStatus(true);
            thirdPartyCarsRepository.save(ss);
            res.setResult(0);
            res.setMessage("?????????????? ????????????????");
        }
        return res;
    }


}

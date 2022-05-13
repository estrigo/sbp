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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Log
@Service
public class ThirdPartyPaymentServiceImpl implements ThirdPartyPaymentService {

    private ThirdPartyCarsRepository thirdPartyCarsRepository;
    private RootServicesGetterService rootServicesGetterService;
    private ThirdPartyPaymentRepository thirdPartyPaymentRepository;

    public ThirdPartyPaymentServiceImpl (RootServicesGetterService rootServicesGetterService,
                                         ThirdPartyCarsRepository thirdPartyCarsRepository,
                                         ThirdPartyPaymentRepository thirdPartyPaymentRepository) {
        this.rootServicesGetterService = rootServicesGetterService;
        this.thirdPartyCarsRepository = thirdPartyCarsRepository;
        this.thirdPartyPaymentRepository = thirdPartyPaymentRepository;
    }

    SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormatTZ);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Boolean checkCarIfThirdPartyPayment (String plateNumber){
        ThirdPartyCars thirdPartyCars = thirdPartyCarsRepository.findByPlateNumber(plateNumber);
        if (thirdPartyCars != null && thirdPartyCars.getCar_number()!=null && thirdPartyCars.getType().equals("direct")) {
            return true;
        } else {
            return false;
        }
    }

    public void saveThirdPartyPayment (String plateNumber, Date entryDate, Date exitDate, BigDecimal rate, String parkingUid) {
        ThirdPartyPayment thirdPartyPayment = new ThirdPartyPayment();
        thirdPartyPayment.setCar_number(plateNumber);
        thirdPartyPayment.setEntryDate(entryDate);
        thirdPartyPayment.setExitDate(exitDate);
        thirdPartyPayment.setRateAmount(rate);
        thirdPartyPaymentRepository.save(thirdPartyPayment);
        log.info("Payment rate sent to third party.");
        sendPayment(plateNumber, entryDate, exitDate, rate, parkingUid);
    }

    private void sendPayment(String plateNumber, Date entryDate, Date exitDate, BigDecimal rate, String parkingUid) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://mega.parqour.com/mega/client/notify";
        Map<String, String> params = new HashMap<>();
        params.put("plate", plateNumber);
        params.put("sum", String.valueOf(rate));
        params.put("in_date", format.format(entryDate));
        params.put("out_date", format.format(exitDate));
        params.put("message", "Сумма оплаты по безакцептному методу");
        params.put("parking_uid", parkingUid);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity request = new HttpEntity<>(params, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class );
        log.info("Response: " + responseEntity.getBody());
    }

    public ResponseThPP removeClient(RequestThPP requestThPP) {
        ThirdPartyCars thirdPartyCars = thirdPartyCarsRepository.findByPlateNumber(requestThPP.getPlatenumber());
        ResponseThPP res = new ResponseThPP();
        if (thirdPartyCars != null && requestThPP.getCommand().equals("delete")) {
            thirdPartyCarsRepository.delete(thirdPartyCars);
            res.setResult(0);
            res.setMessage("успешно изменен");
        } else if (thirdPartyCars != null && requestThPP.getCommand().equals("freeze")) {
            thirdPartyCars.setStatus(false);
            thirdPartyCarsRepository.save(thirdPartyCars);
            res.setResult(0);
            res.setMessage("успешно изменен");
        }
        else {
            res.setResult(1);
            res.setMessage("машина с таким номером не существует");
        }
        return res;
    }

    public ResponseThPP addClient(RequestThPP requestThPP) {
        ThirdPartyCars thirdPartyCars = thirdPartyCarsRepository.findByPlateNumber(requestThPP.getPlatenumber());
        ResponseThPP res = new ResponseThPP();
        if(thirdPartyCars != null && requestThPP.getCommand().equals("add") &&
                !requestThPP.getType().equals(thirdPartyCars.getType())) {
            thirdPartyCars.setType(requestThPP.getType());
            thirdPartyCarsRepository.save(thirdPartyCars);
            res.setResult(1);
            res.setMessage("номер успешно переведен на другой тип оплаты");
        } else if (thirdPartyCars != null && requestThPP.getCommand().equals("add") &&
                requestThPP.getType().equals(thirdPartyCars.getType())) {
            res.setResult(2);
            res.setMessage("машина с таким номером уже существует");
        } else if (thirdPartyCars != null && requestThPP.getCommand().equals("restore")) {
            thirdPartyCars.setStatus(true);
            thirdPartyCarsRepository.save(thirdPartyCars);
            res.setResult(0);
            res.setMessage("успешно добавлен");
        }
        else {
            ThirdPartyCars ss = new ThirdPartyCars();
            ss.setCar_number(requestThPP.getPlatenumber());
            ss.setType(requestThPP.getType());
            ss.setStatus(true);
            thirdPartyCarsRepository.save(ss);
            res.setResult(0);
            res.setMessage("успешно добавлен");
        }
        return res;
    }



}

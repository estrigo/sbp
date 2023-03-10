package kz.spt.megaplugin.controller;

import kz.spt.megaplugin.model.RequestThPP;
import kz.spt.megaplugin.model.ResponseThPP;
import kz.spt.megaplugin.service.ThirdPartyPaymentService;
import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;

@RestController
@Log
@RequestMapping("/thirdpartypayment")
public class ThirdPartyPaymentController {

    private ThirdPartyPaymentService thirdPartyPaymentService;

    public ThirdPartyPaymentController (ThirdPartyPaymentService thirdPartyPaymentService) {
        this.thirdPartyPaymentService = thirdPartyPaymentService;
    }

    @PostMapping(value = "/client/add", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addClient(@RequestBody RequestThPP requestThPP) {
        try {
            if (requestThPP != null) {
                if (requestThPP.getType() == null){
                    throw new Exception("Не указан тип оплаты!");
                } else if (requestThPP.getPlatenumber() == null){
                    throw new Exception("Не указан номер машины!");
                } else if (requestThPP.getCommand() == null) {
                    throw new Exception("Не указана команда!");
                }
            }
            ResponseThPP res = thirdPartyPaymentService.addClient(requestThPP);
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseThPP(null, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(value = "/client/change", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity removeClient(@RequestBody RequestThPP requestThPP) {
        try {
            if (requestThPP.getPlatenumber() == null){
                throw new Exception("Не указан номер машины!");
            }
            ResponseThPP res = thirdPartyPaymentService.removeClient(requestThPP);
            return new ResponseEntity<>(res, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new ResponseThPP(null, e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

}

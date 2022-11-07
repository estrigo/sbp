package kz.spt.app.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.spt.lib.exception.ErrorMessage;
import kz.spt.lib.exception.TerminalStatusException;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.parkomat.CarInRequestDTO;
import kz.spt.lib.model.dto.parkomat.ParkomatCommandDTO;
import kz.spt.lib.model.dto.parkomat.InCarResponseDTO;
import kz.spt.lib.service.CarStateService;
import kz.spt.lib.service.PaymentService;
import kz.spt.lib.service.PosTerminalService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping(value = "/parkomat")
@AllArgsConstructor
public class ParkomatRestController {

    private CarStateService carStateService;
    private PaymentService paymentService;
    private PosTerminalService posTerminalService;

    @RequestMapping(value = "/lastcars", method = RequestMethod.POST, consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object getLastCars(@Valid CarInRequestDTO checkCarInDTO) throws Exception {
        Iterable<CarState> carStates = carStateService.getCurrentNotPayed(checkCarInDTO.getCar_number());
        return StreamSupport.stream(carStates.spliterator(), false).map(carState -> InCarResponseDTO.info(carState)).collect(Collectors.toList());
    }


    @PostMapping(value = "/check", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object getBillingInfo(ParkomatCommandDTO parkomatCommandDto) throws Exception{
        return paymentService.billingInteractions(parkomatCommandDto);
    }

    @PostMapping(value = "/zreport", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object getZReport(ParkomatCommandDTO parkomatCommandDto) throws Exception{
        parkomatCommandDto.setCommand("zReport");
        ObjectNode result = (ObjectNode) paymentService.billingInteractions(parkomatCommandDto);
        if (result!= null && result.has("result")) {
            return result.get("result").textValue();
        }
        return null;
    }

    @GetMapping(value = "/status")
    public ResponseEntity<ErrorMessage> getStatusOfReconsilation() throws TerminalStatusException {
        return posTerminalService.checkNotClosedTerminals();
    }

}

package kz.spt.prkstatusplugin.controller;

import kz.spt.prkstatusplugin.model.PaymentProvider;
import kz.spt.prkstatusplugin.service.ParkomatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping(value = "/prkstatus/log")
public class ParkomatLogController {

    private ParkomatService parkomatService;

    public ParkomatLogController(ParkomatService parkomatService) {
        this.parkomatService = parkomatService;
    }
    private String journalUrl = "/get_jurnal";

    @GetMapping("/list")
    public String indexAction(Model model, @RequestParam(value = "parkomat", required = false) String parkomatIP) {


        List<PaymentProvider> paymentProviderList = (List<PaymentProvider>) parkomatService.getParkomatProviders();
        model.addAttribute("parkomatList", paymentProviderList);


        if (parkomatIP != null) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                LocalDate now = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                String formattedDate = now.format(formatter);
                final String baseUrl = "http://" + parkomatIP + ":4000" + journalUrl + "?date=" + formattedDate;
                URI uri = new URI(baseUrl);

                ResponseEntity<String> result = restTemplate.getForEntity(uri, String.class);
                if (result!=null && result.getStatusCode()== HttpStatus.OK) {
                    model.addAttribute("log", result.getBody());
                    model.addAttribute("parkomatIP", parkomatIP);
                } else {
                    model.addAttribute("log", "");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


        return "prkstatus/log/list";
    }

}

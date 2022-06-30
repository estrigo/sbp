package kz.spt.prkstatusplugin.rest;

import kz.spt.prkstatusplugin.enums.SoftwareType;
import kz.spt.prkstatusplugin.model.ParkomatConfig;
import kz.spt.prkstatusplugin.model.ParkomatUpdate;
import kz.spt.prkstatusplugin.model.PaymentProvider;
import kz.spt.prkstatusplugin.model.dto.ParkomatUpdateDTO;
import kz.spt.prkstatusplugin.repository.ParkomatUpdateRepository;
import kz.spt.prkstatusplugin.service.ParkomatService;
import kz.spt.prkstatusplugin.service.ParkomatUpdateFileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/rest/parkomat_updates")
public class ParkomatUpdateRestController {

    private ParkomatService parkomatService;
    private ParkomatUpdateFileService parkomatUpdateFileService;

    public ParkomatUpdateRestController(ParkomatService parkomatService, ParkomatUpdateFileService parkomatUpdateFileService) {
        this.parkomatService = parkomatService;
        this.parkomatUpdateFileService = parkomatUpdateFileService;
    }

    @GetMapping("/list")
    public ResponseEntity<List<ParkomatUpdateDTO>> getUpdateList(@RequestParam("type") String type) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<ParkomatUpdateDTO> list = parkomatService.getUpdates(SoftwareType.valueOf(type), 1).stream().map(p -> ParkomatUpdateDTO.builder()
                .id(p.getId())
                .date(formatter.format(p.getCreated()))
                .type(p.getType().name())
                .file(p.getId().toString())
                .build()
        ).collect(Collectors.toList());

        return new ResponseEntity(list, HttpStatus.OK);
    }

    @GetMapping("/download_update/{fileId}")
    public ResponseEntity downloadUpdate(@PathVariable("fileId") long fileId) throws IOException {
        ParkomatUpdate parkomatUpdate = parkomatService.getUpdate(fileId);
        if (parkomatUpdate != null) {
            File file = parkomatUpdateFileService.getFile(parkomatUpdate);
            if (file.exists()) {
                Path path = Paths.get(file.getAbsolutePath());
                ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileId + ".zip\"")
                        .body(resource);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/isBusy")
    public ResponseEntity checkIsBusy(HttpServletRequest httpRequest) {

        String remoteIP = httpRequest.getRemoteAddr();

        PaymentProvider paymentProvider = parkomatService.getParkomatByIP(remoteIP);
        if (paymentProvider!=null) {

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> result = restTemplate.getForEntity("http://" + paymentProvider.getParkomatIP()+":4000/busy", String.class);
            if (result.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok().body(result.getBody());
            }
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/reload")
    public ResponseEntity reloadParkomat(@RequestParam("parkomatIP") String parkomatIP) {
        PaymentProvider paymentProvider = parkomatService.getParkomatByIP(parkomatIP);
        if (paymentProvider!=null) {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> result = restTemplate.getForEntity("http://" + paymentProvider.getParkomatIP()+":4001/update", String.class);
            if (result.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok().body(result.getBody());
            }
        }
        return ResponseEntity.badRequest().build();

    }

    @GetMapping("/get_config")
    public ResponseEntity getConfig(HttpServletRequest httpRequest) {
        String remoteIP = httpRequest.getRemoteAddr();
        ParkomatConfig config = parkomatService.getParkomatConfig(remoteIP);
        String configText = config.getConfig();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy.HH.mm.ss");
        configText+="\nconfigDate="+formatter.format(config.getUpdated());
        return ResponseEntity.ok().body(configText);

    }

}

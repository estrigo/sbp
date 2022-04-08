package kz.spt.app.rest;

import kz.spt.lib.model.dto.CalibrationDto;
import kz.spt.lib.service.CalibrationService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/rest/camera")
@RequiredArgsConstructor
public class CameraRestController {
    private final CalibrationService calibrationService;

    @GetMapping(value = "/getCalibrationById/{cameraId}")
    public String getCalibrationById(@PathVariable("cameraId") Long cameraId) {
        return calibrationService.findByCamera(cameraId).map(m -> m.getJson()).orElse("");
    }

    @GetMapping(value = "/getCalibrationByIp/{ip}")
    public String getCalibrationByIp(@PathVariable("ip") String ip) {
        return calibrationService.findByIp(ip).map(m -> StringUtils.isEmpty(m.getJson()) ? "{}" : m.getJson()).orElse("{}");
    }

    @PostMapping(value = "/calibration/save/{cameraId}")
    public String saveCalibration(@PathVariable("cameraId") Long cameraId, @RequestBody String json) {
        calibrationService.save(cameraId, json);
        return json;
    }
}

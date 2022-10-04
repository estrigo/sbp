package kz.spt.app.rest;

import kz.spt.lib.model.dto.adminPlace.AdminCommandDto;
import kz.spt.lib.service.AdminService;
import kz.spt.lib.service.WhiteListJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping(value = "/rest/admin")
@RequiredArgsConstructor
public class AdminRestController {

    private final AdminService adminService;

    private final WhiteListJob whiteListJob;



    @RequestMapping(value = "/executor", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> executor(@RequestBody AdminCommandDto commandDto){
        switch (commandDto.getType()) {
            case GIT_INFO:
                return adminService.getBasicResponse();
            case START_WHITE_LIST_JOB:
              return   whiteListJob.startWhiteListJob();
            case STOP_WHITE_LIST_JOB:
                return whiteListJob.stopWhiteListJob();
            case UPDATE_PROPERTY:
                return adminService.updateProperty(commandDto);
            default:
                return null;
        }
    }
}

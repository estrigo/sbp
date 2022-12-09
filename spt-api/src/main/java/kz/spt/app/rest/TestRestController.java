package kz.spt.app.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/rest/test")
public class TestRestController {

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping (value = "/check/{timeout}")
    public String manualEnter(@PathVariable("timeout") Long timeout) throws InterruptedException {
        Thread.sleep(timeout);
        return "ok";
    }
}

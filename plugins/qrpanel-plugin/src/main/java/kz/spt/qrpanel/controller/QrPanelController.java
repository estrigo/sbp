package kz.spt.qrpanel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/qrpanel")
public class QrPanelController {

    @GetMapping
    public String showQrPage(Model model)
    {
        return "qrpanel/index";
    }


}

package kz.spt.qrpanel.controller;

import kz.spt.lib.model.Gate;
import kz.spt.qrpanel.model.GateOut;
import kz.spt.qrpanel.repository.GateOutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/qrpanel")
public class QrPanelController {

    @Autowired
    GateOutRepository gateOutRepository;
    @GetMapping
    public String showQrPage(Model model)
    {
        List<GateOut> gateOutList = (List<GateOut>) gateOutRepository.findByGateType(GateOut.GateType.OUT);
        model.addAttribute("gateList", gateOutList);
        return "qrpanel/index";
    }


}

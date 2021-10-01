package kz.spt.billingplugin.controller;

import kz.spt.billingplugin.dto.PaymentProviderDTO;
import kz.spt.billingplugin.dto.TableDataDTO;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.service.PaymentProviderService;
import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/billing/providers")
public class ProviderController {

    private PaymentProviderService paymentProviderService;

    public ProviderController(PaymentProviderService paymentProviderService) {
        this.paymentProviderService = paymentProviderService;
    }

    @GetMapping("/list")
    public String showAllProviders(Model model) {
        return "/billing/providers/list";
    }

    @PostMapping("/list")
    public ResponseEntity getData() {
        List<PaymentProvider> paymentProviderList = (List<PaymentProvider>) paymentProviderService.listAllPaymentProviders();
        TableDataDTO tableDataDTO = new TableDataDTO();
        tableDataDTO.setDraw(1);
        tableDataDTO.setRecordsTotal(10);
        tableDataDTO.setRecordsFiltered(1);
        tableDataDTO.setData(paymentProviderList.stream().map(paymentProvider -> PaymentProviderDTO.convertToDto(paymentProvider)).collect(Collectors.toList()));
        return new ResponseEntity(tableDataDTO, HttpStatus.OK);
    }

    @GetMapping("/edit/{providerId}")
    public String getEditingProviderId(Model model, @PathVariable Long providerId) {
        model.addAttribute("paymentProvider", paymentProviderService.getProviderById(providerId));
        return "billing/providers/edit";
    }

    @PostMapping("/save/{providerId}")
    public String providerEdit(@PathVariable Long providerId, @Valid PaymentProvider paymentProvider, BindingResult bindingResult) throws NoSuchAlgorithmException {

        if (!bindingResult.hasErrors()) {
            paymentProviderService.saveProvider(paymentProvider);
        }
        return "redirect:/billing/providers/list";
    }

    @GetMapping("/new/provider")
    public String getEditingProviderId(Model model) {
        PaymentProvider paymentProvider = new PaymentProvider();
        model.addAttribute("paymentProvider", paymentProvider);
        return "billing/providers/add";
    }

    @PostMapping("/new/save")
    public String providerAdd(@Valid PaymentProvider paymentProvider, BindingResult bindingResult) throws NoSuchAlgorithmException {

        if (!bindingResult.hasErrors()) {
            paymentProviderService.saveProvider(paymentProvider);
        }
        return "redirect:/billing/providers/list";
    }
}

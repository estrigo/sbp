package kz.spt.billingplugin.controller;


import kz.spt.billingplugin.BillingPlugin;
import kz.spt.billingplugin.service.PaymentProviderService;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.model.PaymentCheckLog;
import kz.spt.lib.model.dto.SelectOption;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/billing/payments")
public class PaymentController {

    private PaymentService paymentService;
    private PaymentProviderService paymentProviderService;
    private RootServicesGetterService rootServicesGetterService;


    public PaymentController(PaymentService paymentService, PaymentProviderService paymentProviderService,
                             RootServicesGetterService rootServicesGetterService) {
        this.paymentService = paymentService;
        this.paymentProviderService = paymentProviderService;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    @GetMapping("/list")
    public String showAllPayments(Model model) {
        model.addAttribute("paymentProviders", paymentProviderService.getSelectOption());
        return "/billing/payments/list";
    }

    @GetMapping("/log")
    public String showLogs(Model model, @RequestParam(value = "value", required = false) Long value) {
        List<SelectOption> selectOptions = paymentProviderService.getSelectOption();
        selectOptions.add(new SelectOption("0", "Нет значения"));
        model.addAttribute("paymentProviders", selectOptions);
        if (!ObjectUtils.isEmpty(value)) {
            SelectOption option =
                    selectOptions.stream()
                            .filter(o -> o.value != null && Long.valueOf(o.value).equals(value))
                            .findFirst().orElse(null);
            Long paymentProviderId = value.equals(0L) ? null : value;

            List<PaymentCheckLog> paymentCheckLogs = getRootServicesGetterService().getPaymentCheckLogService()
                    .finPaymentCheckLogByProviderId(paymentProviderId, PageRequest.of(0, 100, Sort.by("id").descending()));
            List<String> logs = paymentCheckLogs.stream().map(paymentService::toLog).collect(Collectors.toList());
            model.addAttribute("logs", logs);
            model.addAttribute("label", option == null || option.value == null ? "" : option.label);
        }
        return "/billing/payments/log";
    }

    private RootServicesGetterService getRootServicesGetterService() {
        if (rootServicesGetterService == null) {
            rootServicesGetterService = (RootServicesGetterService) BillingPlugin.INSTANCE.getApplicationContext().getBean("rootServicesGetterServiceImpl");
        }
        return rootServicesGetterService;
    }
}
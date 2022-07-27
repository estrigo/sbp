package kz.spt.app.rest;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.lib.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/rest/payments")
public class PaymentsRestController {

    private final PaymentService paymentService;

    @GetMapping("statements")
    public JsonNode getPaymentStatements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam Boolean onlyTransactionId,
            @RequestParam String providerName) throws Exception {
        return paymentService.findAllByCreatedBetweenAndProviderName(
                dateFrom,
                dateTo,
                onlyTransactionId,
                providerName);
    }

    @GetMapping("result")
    public JsonNode getPaymentResult(
            @RequestParam String transactionId,
            @RequestParam String providerName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime transactionTime) throws Exception {
        return paymentService.findFirstByTransactionAndProviderNameAndCreated(
                transactionId,
                providerName,
                transactionTime);
    }

    @PutMapping("cancel/{transactionId}/{reason}")
    public void cancelPayment(
            @PathVariable String transactionId,
            @PathVariable String reason) throws Exception {
        paymentService.cancelTransactionByTrxId(
                transactionId,
                reason);
    }

}

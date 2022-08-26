package kz.spt.billingplugin.service;

import kz.spt.billingplugin.dto.FilterPaymentDTO;
import kz.spt.billingplugin.dto.PaymentLogDTO;
import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.PaymentCheckLog;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface PaymentService {

    Iterable<Payment> listAllPayments();

    Payment savePayment(Payment payment);

    List<Payment> getPaymentsByCarStateId(Long carStateId);

    void updateOutTimestamp(Long carStateId, Date outTimestamp);

    Page<PaymentLogDTO> getPaymentDtoList(PagingRequest pagingRequest) throws ParseException;

    List<PaymentLogDTO> getPaymentDtoExcelList(FilterPaymentDTO filter);

    List<Payment> findByTransactionAndProvider(String transaction, PaymentProvider paymentProvider);

    List<Payment> findByTransaction(String transaction);

    String toLog(PaymentCheckLog log);

    void cancelPaymentByTransactionId(String transaction, String reason) throws Exception;
}

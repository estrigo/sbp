package kz.spt.billingplugin.service;

import kz.spt.billingplugin.dto.BalanceDebtLogDto;
import kz.spt.billingplugin.dto.TransactionDto;
import kz.spt.billingplugin.dto.TransactionFilterDto;
import kz.spt.billingplugin.model.Balance;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

public interface BalanceService {

    BigDecimal addBalance(String plateNumber, BigDecimal value, Long carStateId, String description,
                          String descriptionRu, String descriptionLocal, String provider, Boolean isAbonomentPayment);

    BigDecimal subtractBalance(String plateNumber, BigDecimal value, Long carStateId, String description,
                               String descriptionRu, String descriptionLocal, String provider, Boolean isAbonomentPayment);

    BigDecimal getBalance(String plateNumber);

    org.springframework.data.domain.Page<Balance> filterBalances(String plateNumber, PagingRequest pagingRequest);

    Page<Balance> getBalanceList(PagingRequest pagingRequest, String plateNumber);

    void deleteAllDebts();

    Page<TransactionDto> getTransactionList(PagingRequest pagingRequest, TransactionFilterDto dto) throws ParseException;

    Page<BalanceDebtLogDto> getClearedDebtList(PagingRequest pagingRequest, String date) throws ParseException;

    Boolean changeTransactionAmount(Long id, BigDecimal amount);

    Boolean showBalanceDebtLog();
}

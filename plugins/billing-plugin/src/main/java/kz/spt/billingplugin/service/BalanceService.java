package kz.spt.billingplugin.service;

import kz.spt.billingplugin.dto.BalanceDebtLogDto;
import kz.spt.billingplugin.dto.TransactionDto;
import kz.spt.billingplugin.dto.TransactionFilterDto;
import kz.spt.billingplugin.model.Balance;
import kz.spt.billingplugin.model.Transaction;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;

public interface BalanceService {

    BigDecimal addBalance(String plateNumber, BigDecimal value, Long carStateId, String description,
                          String descriptionRu, String provider);

    BigDecimal subtractBalance(String plateNumber, BigDecimal value, Long carStateId, String description,
                               String descriptionRu, String provider);

    BigDecimal getBalance(String plateNumber);

    List<Balance> listAllBalances();

    Page<Balance> getBalanceList(PagingRequest pagingRequest);

    void deleteAllDebts();

    Page<TransactionDto> getTransactionList(PagingRequest pagingRequest, TransactionFilterDto dto) throws ParseException;

    Page<BalanceDebtLogDto> getClearedDebtList(PagingRequest pagingRequest, String date) throws ParseException;

    Boolean changeTransactionAmount(Long id, BigDecimal amount);

    Boolean showBalanceDebtLog();
}

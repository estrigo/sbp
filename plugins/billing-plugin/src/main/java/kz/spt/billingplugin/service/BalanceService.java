package kz.spt.billingplugin.service;

import kz.spt.billingplugin.model.Balance;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;

import java.math.BigDecimal;
import java.util.List;

public interface BalanceService {

    BigDecimal addBalance(String plateNumber, BigDecimal value, Long carStateId, String description, String descriptionRu);

    BigDecimal subtractBalance(String plateNumber, BigDecimal value, Long carStateId, String description, String descriptionRu);

    BigDecimal getBalance(String plateNumber);

    List<Balance> listAllBalances();

    Page<Balance> getBalanceList(PagingRequest pagingRequest);

    void deleteAllDebts();
}

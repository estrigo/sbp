package kz.spt.billingplugin.service;

import java.math.BigDecimal;

public interface BalanceService {

    BigDecimal addBalance(String plateNumber, BigDecimal value);

    BigDecimal subtractBalance(String plateNumber, BigDecimal value);

    BigDecimal getBalance(String plateNumber);
}

package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.model.Balance;
import kz.spt.billingplugin.repository.BalanceRepository;
import kz.spt.billingplugin.service.BalanceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BalanceServiceImpl implements BalanceService {

    private BalanceRepository balanceRepository;

    public BalanceServiceImpl(BalanceRepository balanceRepository){
        this.balanceRepository = balanceRepository;
    }

    @Override
    public BigDecimal addBalance(String plateNumber, BigDecimal value) {
        Balance balance;
        Optional<Balance> optionalBalance = balanceRepository.findById(plateNumber);
        if(optionalBalance.isPresent()){
            balance = optionalBalance.get();
            balance.setBalance(balance.getBalance().add(value));
        } else {
            balance = new Balance();
            balance.setPlateNumber(plateNumber);
            balance.setBalance(value);
        };
        Balance savedBalance = balanceRepository.save(balance);
        return savedBalance.getBalance();
    }

    @Override
    public BigDecimal subtractBalance(String plateNumber, BigDecimal value) {
       return addBalance(plateNumber, value.multiply(new BigDecimal(-1)));
    }

    @Override
    public BigDecimal getBalance(String plateNumber) {
        plateNumber = plateNumber.toUpperCase();
        Optional<Balance> optionalBalance = balanceRepository.findById(plateNumber);

        if(optionalBalance.isPresent()){
            return optionalBalance.get().getBalance();
        }
        return new BigDecimal(0);
    }
}

package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.bootstrap.datatable.BalanceComparators;
import kz.spt.billingplugin.model.Balance;
import kz.spt.billingplugin.repository.BalanceRepository;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class BalanceServiceImpl implements BalanceService {

    private BalanceRepository balanceRepository;

    public BalanceServiceImpl(BalanceRepository balanceRepository){
        this.balanceRepository = balanceRepository;
    }

    private static final Comparator<Balance> EMPTY_COMPARATOR = (e1, e2) -> 0;

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

    @Override
    public List<Balance> listAllBalances() {
        return balanceRepository.findAll();
    }

    @Override
    public Page<Balance> getBalanceList(PagingRequest pagingRequest) {
        List<Balance> allBalances = (List<Balance>) this.listAllBalances();
        return getPage(allBalances, pagingRequest);
    }

    private Page<Balance> getPage(List<Balance> balancesList, PagingRequest pagingRequest) {
        List<Balance> filtered = balancesList.stream()
                .sorted(sortBalance(pagingRequest))
                .filter(filterBalance(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = balancesList.stream()
                .filter(filterBalance(pagingRequest))
                .count();

        Page<Balance> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<Balance> filterBalance(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch().getValue())) {
            return paymentLogDTOs -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return balances -> (balances.getPlateNumber() !=  null && balances.getPlateNumber().toLowerCase().contains(value.toLowerCase())
                || (balances.getBalance() != null && balances.getBalance().toString().toLowerCase().contains(value.toLowerCase())));
    }

    private Comparator<Balance> sortBalance(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<Balance> comparator = BalanceComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}
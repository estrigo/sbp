package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.bootstrap.datatable.BalanceComparators;
import kz.spt.billingplugin.dto.TransactionDto;
import kz.spt.billingplugin.dto.TransactionFilterDto;
import kz.spt.billingplugin.model.Balance;
import kz.spt.billingplugin.model.Transaction;
import kz.spt.billingplugin.model.TransactionSpecification;
import kz.spt.billingplugin.repository.BalanceRepository;
import kz.spt.billingplugin.repository.TransactionRepository;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.CarState;
import kz.spt.lib.service.CarStateService;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
public class BalanceServiceImpl implements BalanceService {

    private BalanceRepository balanceRepository;
    private TransactionRepository transactionRepository;
    private RootServicesGetterService rootServicesGetterService;

    public BalanceServiceImpl(BalanceRepository balanceRepository, TransactionRepository transactionRepository,
                              RootServicesGetterService rootServicesGetterService) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    private static final Comparator<Balance> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private static final Comparator<Transaction> TRANSACTION_EMPTY_COMPARATOR = (e1, e2) -> 0;

    @Override
    public BigDecimal addBalance(String plateNumber, BigDecimal value, Long carStateId, String description, String descriptionRu,
                                 String provider) {
        Balance balance;
        Optional<Balance> optionalBalance = balanceRepository.findById(plateNumber);
        if (optionalBalance.isPresent()) {
            balance = optionalBalance.get();
            balance.setBalance(balance.getBalance().add(value));
        } else {
            balance = new Balance();
            balance.setPlateNumber(plateNumber);
            balance.setBalance(value);
        }
        ;
        Balance savedBalance = balanceRepository.save(balance);

        Transaction transaction = new Transaction(plateNumber, value, carStateId, description, descriptionRu,
                provider, getBalance(plateNumber));
        transactionRepository.save(transaction);

        return savedBalance.getBalance();
    }

    @Override
    public BigDecimal subtractBalance(String plateNumber, BigDecimal value, Long carStateId, String description,
                                      String descriptionRu, String provider) {
        return addBalance(plateNumber, BigDecimal.ZERO.compareTo(value) > 0 ? value : value.multiply(new BigDecimal(-1)), carStateId, description, descriptionRu,
                provider);
    }

    @Override
    public BigDecimal getBalance(String plateNumber) {
        plateNumber = plateNumber.toUpperCase();
        Optional<Balance> optionalBalance = balanceRepository.findById(plateNumber);

        if (optionalBalance.isPresent()) {
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

    @Override
    public void deleteAllDebts() {
        List<Balance> debtBalances = balanceRepository.debtBalances();
        for (Balance balance : debtBalances) {
            balance.setBalance(BigDecimal.ZERO);
            balanceRepository.save(balance);
        }
    }

    @Override
    public Page<TransactionDto> getTransactionList(PagingRequest pagingRequest, TransactionFilterDto dto) throws ParseException {
        org.springframework.data.domain.Page<Transaction> transactions = listByFilters(dto, pagingRequest);
        return getTransactionPage(transactions, pagingRequest);
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

        return balances -> (balances.getPlateNumber() != null && balances.getPlateNumber().toLowerCase().contains(value.toLowerCase())
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

    private List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    public org.springframework.data.domain.Page<Transaction> listByFilters(TransactionFilterDto transactionFilterDto, PagingRequest pagingRequest) throws ParseException {
        Specification<Transaction> specification = getTransactionSpecification(transactionFilterDto);

        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData();  // Дата и время, ГНРЗ, сумма, Описание
        Direction dir = order.getDir();

        Sort sort = null;
        if ("plateNumber".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("plateNumber").descending();
            } else {
                sort = Sort.by("plateNumber").ascending();
            }
        } else if ("amount".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("amount").descending();
            } else {
                sort = Sort.by("amount").ascending();
            }
        } else if ("date".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("date").descending();
            } else {
                sort = Sort.by("date").ascending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        return transactionRepository.findAll(specification, rows);
    }

    private Specification<Transaction> getTransactionSpecification(TransactionFilterDto transactionFilterDto) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Specification<Transaction> specification = null;

        if (transactionFilterDto.toDate != null && !"".equals(transactionFilterDto.toDate)) {
            specification = TransactionSpecification.lessDate(format.parse(transactionFilterDto.toDate));
        }
        if (transactionFilterDto.fromDate != null && !"".equals(transactionFilterDto.fromDate)) {
            specification = specification == null ? TransactionSpecification.greaterDate(format.parse(transactionFilterDto.fromDate)) : specification.and(TransactionSpecification.greaterDate(format.parse(transactionFilterDto.fromDate)));
        }
        if (transactionFilterDto.plateNumber != null && !"".equals(transactionFilterDto.plateNumber)) {
            specification = specification == null ? TransactionSpecification.likePlateNumber(transactionFilterDto.plateNumber) : specification.and(TransactionSpecification.likePlateNumber(transactionFilterDto.plateNumber));
        }
        if (transactionFilterDto.amount != null && !"".equals(transactionFilterDto.amount)) {
            specification = specification == null ? TransactionSpecification.equalAmount(transactionFilterDto.amount) : specification.and(TransactionSpecification.equalAmount(transactionFilterDto.amount));
        }
        return specification;
    }

    @Override
    public Boolean changeTransactionAmount(Long id, BigDecimal amount) {
        Transaction transaction = transactionRepository.findById(id).get();
        BigDecimal oldAmount = transaction.getAmount();

        if (oldAmount.compareTo(BigDecimal.ZERO) <= 0 && amount.compareTo(BigDecimal.ZERO) > 0) {
            return false;
        }
        if (oldAmount.compareTo(BigDecimal.ZERO) >= 0 && amount.compareTo(BigDecimal.ZERO) < 0) {
            return false;
        }

        Balance balance = balanceRepository.getBalanceByPlateNumber(transaction.getPlateNumber());
        BigDecimal balanceDiff = BigDecimal.valueOf(0);
        if (oldAmount.compareTo(BigDecimal.ZERO) < 0) {
            balance.setBalance(balance.getBalance().add(oldAmount.multiply(BigDecimal.valueOf(-1))).add(amount));
            transaction.setAmount(amount);
//            balanceDiff = oldAmount.multiply(BigDecimal.valueOf(-1)).add(amount);
            balanceDiff = oldAmount.subtract(amount);
        }
        if (oldAmount.compareTo(BigDecimal.ZERO) > 0) {
            balance.setBalance(balance.getBalance().subtract(oldAmount).add(amount));
            transaction.setAmount(amount);
            balanceDiff = oldAmount.subtract(amount);
        }
        if (!balanceDiff.equals(0)) {
            List<Transaction> transactionList = transactionRepository.findAllByPlateNumberAndDateAfter(
                    transaction.getPlateNumber(), transaction.getDate());
            final BigDecimal finalBalanceDiff = balanceDiff;
            transactionList
                    .forEach(trn -> {
                        trn.setRemainder(trn.getRemainder().subtract(finalBalanceDiff));
                    });
            transactionRepository.saveAll(transactionList);
        }
        balanceRepository.save(balance);
        transaction.setRemainder(transaction.getRemainder().subtract(balanceDiff));
        transactionRepository.save(transaction);
        return true;
    }

    private Page<TransactionDto> getTransactionPage(org.springframework.data.domain.Page<Transaction> transactionsList, PagingRequest pagingRequest) {

        CarStateService carStateService = rootServicesGetterService.getCarStateService();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        List<TransactionDto> filteredDto = new ArrayList<>(transactionsList.getSize());
        for (Transaction transaction : transactionsList.toList()) {
            TransactionDto transactionDto = TransactionDto.fromTransaction(transaction);
            if (transaction.getCarStateId() != null) {
                CarState carState = carStateService.findById(transaction.getCarStateId());
                if (carState != null)
                    transactionDto.period = (carState.getParking() != null ? carState.getParking().getName() + " " : "") + sdf.format(carState.getInTimestamp()) + (carState.getOutTimestamp() != null ? " - " + sdf.format(carState.getOutTimestamp()) : "");
            }
            transactionDto.date = sdf.format(transaction.getDate());
            filteredDto.add(transactionDto);
        }

        Page<TransactionDto> page = new Page<>(filteredDto);
        page.setRecordsFiltered((int) transactionsList.getTotalElements());
        page.setRecordsTotal((int) transactionsList.getTotalElements());
        page.setDraw(pagingRequest.getDraw());

        return page;
    }
}

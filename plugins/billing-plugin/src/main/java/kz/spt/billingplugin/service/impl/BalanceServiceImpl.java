package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.bootstrap.datatable.BalanceComparators;
import kz.spt.billingplugin.bootstrap.datatable.TransactionComparators;
import kz.spt.billingplugin.dto.TransactionDto;
import kz.spt.billingplugin.dto.TransactionFilterDto;
import kz.spt.billingplugin.model.Balance;
import kz.spt.billingplugin.model.Transaction;
import kz.spt.billingplugin.model.TransactionSpecification;
import kz.spt.billingplugin.repository.BalanceRepository;
import kz.spt.billingplugin.repository.TransactionRepository;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.EventLog;
import kz.spt.lib.model.EventLogSpecification;
import kz.spt.lib.model.dto.EventFilterDto;
import kz.spt.lib.service.CarStateService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class BalanceServiceImpl implements BalanceService {

    private BalanceRepository balanceRepository;
    private TransactionRepository transactionRepository;
    private RootServicesGetterService rootServicesGetterService;

    public BalanceServiceImpl(BalanceRepository balanceRepository, TransactionRepository transactionRepository,
                              RootServicesGetterService rootServicesGetterService){
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.rootServicesGetterService = rootServicesGetterService;
    }

    private static final Comparator<Balance> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private static final Comparator<Transaction> TRANSACTION_EMPTY_COMPARATOR = (e1, e2) -> 0;

    @Override
    public BigDecimal addBalance(String plateNumber, BigDecimal value, Long carStateId, String description, String descriptionRu) {
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

        Transaction transaction = new Transaction(plateNumber, value, carStateId, description, descriptionRu);
        transactionRepository.save(transaction);

        return savedBalance.getBalance();
    }

    @Override
    public BigDecimal subtractBalance(String plateNumber, BigDecimal value, Long carStateId, String description, String descriptionRu) {
       return addBalance(plateNumber, BigDecimal.ZERO.compareTo(value) > 0 ? value : value.multiply(new BigDecimal(-1)), carStateId, description, descriptionRu);
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

    @Override
    public void deleteAllDebts() {
        List<Balance> debtBalances = balanceRepository.debtBalances();
        for(Balance balance : debtBalances){
            balance.setBalance(BigDecimal.ZERO);
            balanceRepository.save(balance);
        }
    }

    @Override
    public Page<TransactionDto> getTransactionList(PagingRequest pagingRequest, TransactionFilterDto dto) throws ParseException {
        List<Transaction> transactions = (List<Transaction>) this.listByFilters(dto);
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
    
    private List<Transaction> getAllTransactions(){
        return transactionRepository.findAll();
    }

    @Override
    public Iterable<Transaction> listByFilters(TransactionFilterDto transactionFilterDto) throws ParseException {

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
        specification = specification == null ? TransactionSpecification.orderById() : specification.and(TransactionSpecification.orderById());
        if (specification != null) {
            return transactionRepository.findAll(specification);
        } else {
            return transactionRepository.findAll();
        }
    }

    private Page<TransactionDto> getTransactionPage(List<Transaction> transactionsList, PagingRequest pagingRequest) {
        List<Transaction> filtered = transactionsList.stream()
                .sorted(sortTransaction(pagingRequest))
                .filter(filterTransaction(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        CarStateService carStateService = rootServicesGetterService.getCarStateService();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        List<TransactionDto> filteredDto = new ArrayList<>();
        for(Transaction transaction : filtered){
            TransactionDto transactionDto = TransactionDto.fromTransaction(transaction);
            if(transaction.getCarStateId() != null){
                CarState carState = carStateService.findById(transaction.getCarStateId());
                transactionDto.period = (carState.getParking() != null ? carState.getParking().getName() + " " : "") + sdf.format(carState.getInTimestamp()) + (carState.getOutTimestamp() != null ? " - " + sdf.format(carState.getOutTimestamp()) : "");
            }
            transactionDto.date = sdf.format(transaction.getDate());
            filteredDto.add(transactionDto);
        }

        long count = transactionsList.stream()
                .filter(filterTransaction(pagingRequest))
                .count();

        Page<TransactionDto> page = new Page<>(filteredDto);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<Transaction> filterTransaction(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch().getValue())) {
            return transactionDtos -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return transactions -> (transactions.getPlateNumber() !=  null && transactions.getPlateNumber().toLowerCase().contains(value.toLowerCase())
                || (transactions.getAmount() != null && transactions.getAmount().toString().toLowerCase().contains(value.toLowerCase())));
    }

    private Comparator<Transaction> sortTransaction(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return TRANSACTION_EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<Transaction> comparator = TransactionComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, TRANSACTION_EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return TRANSACTION_EMPTY_COMPARATOR;
    }
}

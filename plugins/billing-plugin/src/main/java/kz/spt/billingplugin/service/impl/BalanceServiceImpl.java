package kz.spt.billingplugin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.billingplugin.bootstrap.datatable.BalanceComparators;
import kz.spt.billingplugin.dto.BalanceDebtLogDto;
import kz.spt.billingplugin.dto.TransactionDto;
import kz.spt.billingplugin.dto.TransactionFilterDto;
import kz.spt.billingplugin.model.*;
import kz.spt.billingplugin.repository.BalanceDebtLogRepository;
import kz.spt.billingplugin.repository.BalanceRepository;
import kz.spt.billingplugin.repository.TransactionRepository;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.CarState;
import kz.spt.lib.service.CarStateService;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Log
@Service
@Transactional(noRollbackFor = Exception.class)
public class BalanceServiceImpl implements BalanceService {

    private BalanceRepository balanceRepository;

    private BalanceDebtLogRepository balanceDebtLogRepository;
    private TransactionRepository transactionRepository;
    private RootServicesGetterService rootServicesGetterService;

    @Value("${save.removed.debt.balance}")
    Boolean saveRemovedDebtBalance;

    public BalanceServiceImpl(BalanceRepository balanceRepository, TransactionRepository transactionRepository,
                              RootServicesGetterService rootServicesGetterService, BalanceDebtLogRepository balanceDebtLogRepository) {
        this.balanceRepository = balanceRepository;
        this.transactionRepository = transactionRepository;
        this.rootServicesGetterService = rootServicesGetterService;
        this.balanceDebtLogRepository = balanceDebtLogRepository;
    }

    private static final Comparator<Balance> EMPTY_COMPARATOR = (e1, e2) -> 0;
    private static final Comparator<Transaction> TRANSACTION_EMPTY_COMPARATOR = (e1, e2) -> 0;

    @Override
    public BigDecimal addBalance(String plateNumber, BigDecimal value, Long carStateId, String description, String descriptionRu, String descriptionLocal,
                                 String provider, Boolean isAbonomentPayment) {
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
        Balance savedBalance = balanceRepository.save(balance);

        Transaction transaction = new Transaction(plateNumber, value, carStateId, description, descriptionRu, descriptionLocal,
                provider, getBalance(plateNumber), rootServicesGetterService.getCarService().findByPlatenumber(plateNumber), isAbonomentPayment);
        transactionRepository.save(transaction);

        return savedBalance.getBalance();
    }

    @Override
    public BigDecimal subtractBalance(String plateNumber, BigDecimal value, Long carStateId, String description,
                                      String descriptionRu, String descriptionLocal, String provider, Boolean isAbonomentPayment) {
        return addBalance(plateNumber, BigDecimal.ZERO.compareTo(value) > 0 ? value : value.multiply(new BigDecimal(-1)), carStateId, description, descriptionRu, descriptionLocal,
                provider, isAbonomentPayment);
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
    public org.springframework.data.domain.Page<Balance> filterBalances(String plateNumber, PagingRequest pagingRequest) {
        Specification<Balance> specification = getBalanceSpecification(plateNumber);

        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData();  // ГНРЗ, сумма
        Direction dir = order.getDir();

        Sort sort = null;
        if ("plateNumber".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("plateNumber").descending();
            } else {
                sort = Sort.by("plateNumber").ascending();
            }
        } else if ("balance".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("balance").descending();
            } else {
                sort = Sort.by("balance").ascending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        return balanceRepository.findAll(specification, rows);
    }

    @Override
    public Page<Balance> getBalanceList(PagingRequest pagingRequest, String plateNumber) {
        org.springframework.data.domain.Page<Balance> allBalances = this.filterBalances(plateNumber, pagingRequest);
        return getPage(allBalances, pagingRequest);
    }

    @Override
    public void deleteAllDebts() {
        List<Balance> debtBalances = balanceRepository.debtBalances();
        for (Balance balance : debtBalances) {
            if(saveRemovedDebtBalance){
                createBalanceDebtLog(balance.getPlateNumber(), balance.getBalance());
            }
            balance.setBalance(BigDecimal.ZERO);
            balanceRepository.save(balance);
        }
    }

    @Override
    public Page<TransactionDto> getTransactionList(PagingRequest pagingRequest, TransactionFilterDto dto) throws Exception {
        org.springframework.data.domain.Page<Transaction> transactions = listByFilters(dto, pagingRequest);
        return getTransactionPage(transactions, pagingRequest);
    }

    @Override
    public Page<BalanceDebtLogDto> getClearedDebtList(PagingRequest pagingRequest, String date) throws ParseException {
        org.springframework.data.domain.Page<BalanceDebtLog> balanceDebtLogs = listClearedDebtByFilters(date, pagingRequest);
        return getBalanceDebtLogPage(balanceDebtLogs, pagingRequest);
    }

    private Page<Balance> getPage(org.springframework.data.domain.Page<Balance> balancesList, PagingRequest pagingRequest) {
        List<Balance> filtered = balancesList.stream().collect(Collectors.toList());

        long count = balancesList.stream().count();

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

    public org.springframework.data.domain.Page<BalanceDebtLog> listClearedDebtByFilters(String date, PagingRequest pagingRequest) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(format.parse(date));
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE), 0, 0, 0);
        Date begin = calendar.getTime();
        calendar.add(Calendar.DATE, 1);
        Date end = calendar.getTime();

        Specification<BalanceDebtLog> specification = getBalanceDebtLogSpecification(begin, end);

        Sort sort =  Sort.by("id").ascending();

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        return balanceDebtLogRepository.findAll(specification, rows);
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

    private Specification<BalanceDebtLog> getBalanceDebtLogSpecification(Date begin, Date end) {
        Specification<BalanceDebtLog> specification = BalanceDebtLogSpecification.lessDate(end);
        specification = specification.and(BalanceDebtLogSpecification.greaterDate(begin));

        return specification;
    }

    private Specification<Balance> getBalanceSpecification(String plateNumber) {
        Specification<Balance> specification = null;

        if (plateNumber != null && !"".equals(plateNumber)) {
            specification = specification == null ? BalanceSpecification.likePlateNumber(plateNumber) : specification.and(BalanceSpecification.likePlateNumber(plateNumber));
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

    @Override
    public Boolean showBalanceDebtLog() {
        return saveRemovedDebtBalance;
    }

    private Page<TransactionDto> getTransactionPage(org.springframework.data.domain.Page<Transaction> transactionsList, PagingRequest pagingRequest) throws Exception {

        CarStateService carStateService = rootServicesGetterService.getCarStateService();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        List<TransactionDto> filteredDto = new ArrayList<>(transactionsList.getSize());
        for (Transaction transaction : transactionsList.toList()) {
            TransactionDto transactionDto = TransactionDto.fromTransaction(transaction);

            log.info("transactionDto.platenumber:  " + transaction.getPlateNumber());

            if (transaction.getCarStateId() != null) {
                CarState carState = carStateService.findById(transaction.getCarStateId());
                if (carState != null)
                    transactionDto.period = (carState.getParking() != null ? carState.getParking().getName() + " " : "") + sdf.format(carState.getInTimestamp()) + (carState.getOutTimestamp() != null ? " - " + sdf.format(carState.getOutTimestamp()) : "");
            }
            log.info("transactionDto.period1:  " + transactionDto.period);

            log.info("transaction.getIsAbonomentPayment() != null:  " + (transaction.getIsAbonomentPayment() != null));
            if(transaction.getIsAbonomentPayment() != null && transaction.getIsAbonomentPayment()){
                log.info("transaction.getIsAbonomentPayment(): " + transaction.getIsAbonomentPayment());
                log.info("!transaction.getIsAbonomentPayment(): " + !transaction.getIsAbonomentPayment());
                JsonNode abonoment = rootServicesGetterService.getRootPaymentService().getPaidNotExpiredAbonoment(transaction.getPlateNumber());
                String parkingName = abonoment.get("parkingName").textValue();
                String period = abonoment.get("period").textValue();
                transactionDto.period = parkingName + " " + period;

            }
            log.info("transactionDto.period2:  " + transactionDto.period);
            transactionDto.date = sdf.format(transaction.getDate());
            filteredDto.add(transactionDto);

        }

        Page<TransactionDto> page = new Page<>(filteredDto);
        page.setRecordsFiltered((int) transactionsList.getTotalElements());
        page.setRecordsTotal((int) transactionsList.getTotalElements());
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Page<BalanceDebtLogDto> getBalanceDebtLogPage(org.springframework.data.domain.Page<BalanceDebtLog> balanceDebtLogs, PagingRequest pagingRequest) {

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        List<BalanceDebtLogDto> filteredDto = new ArrayList<>(balanceDebtLogs.getSize());
        for (BalanceDebtLog balanceDebtLog : balanceDebtLogs.toList()) {
            BalanceDebtLogDto balanceDebtLogDto = BalanceDebtLogDto.fromBalanceDebtLog(balanceDebtLog);
            filteredDto.add(balanceDebtLogDto);
        }

        Page<BalanceDebtLogDto> page = new Page<>(filteredDto);
        page.setRecordsFiltered((int) balanceDebtLogs.getTotalElements());
        page.setRecordsTotal((int) balanceDebtLogs.getTotalElements());
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private void createBalanceDebtLog(String plateNumber, BigDecimal balance){
        BalanceDebtLog balanceDebtLog = new BalanceDebtLog();
        balanceDebtLog.setBalance(balance);
        balanceDebtLog.setPlateNumber(plateNumber);
        balanceDebtLogRepository.save(balanceDebtLog);
    }
}

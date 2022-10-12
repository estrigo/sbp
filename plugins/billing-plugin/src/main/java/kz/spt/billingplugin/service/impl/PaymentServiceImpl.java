package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.dto.FilterPaymentDTO;
import kz.spt.billingplugin.dto.HeaderDto;
import kz.spt.billingplugin.dto.PaymentLogDTO;
import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.PaymentSpecification;
import kz.spt.billingplugin.repository.PaymentRepository;
import kz.spt.billingplugin.service.BalanceService;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.billingplugin.service.RootServicesGetterService;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.Property;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import kz.spt.lib.model.PaymentCheckLog;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.util.ObjectUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service("paymentService")
@Transactional(noRollbackFor = Exception.class)
@EnableScheduling
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BalanceService balanceService;
    private final RootServicesGetterService rootServicesGetterService;
    private final TaskScheduler scheduler;
    private ScheduledFuture future;

    private final static String CRON_PR = "payment_register";

    public PaymentServiceImpl(PaymentRepository paymentRepository, BalanceService balanceService,
                              RootServicesGetterService rootServicesGetterService,
                              TaskScheduler scheduler) {
        this.paymentRepository = paymentRepository;
        this.balanceService = balanceService;
        this.rootServicesGetterService = rootServicesGetterService;
        this.scheduler = scheduler;
    }

    @Override
    public Iterable<Payment> listAllPayments() {
        return paymentRepository.listAllPaymentsWithParkings();
    }

    @Override
    public Payment savePayment(Payment payment) {
        return paymentRepository.save(payment);
    }

    private static final Comparator<PaymentLogDTO> EMPTY_COMPARATOR = (e1, e2) -> 0;

    @Override
    public List<Payment> getPaymentsByCarStateId(Long carStateId) {
        return paymentRepository.getPaymentsByCarStateIdWithProvider(carStateId);
    }

    @Override
    public List<Payment> getPaymentsByCarStateId(List<Long> carStateIds) {
        Collection<Long> collection = new ArrayList<Long>(carStateIds);
        return paymentRepository.getPaymentsByCarStateIdWithProvider(collection);
    }

    @Override
    public void updateOutTimestamp(Long carStateId, Date outTimestamp) {
        List<Payment> payments = paymentRepository.getPaymentsByCarStateIdWithProvider(carStateId);
        for (Payment payment : payments) {
            payment.setOutDate(outTimestamp);
        }
        paymentRepository.saveAll(payments);
    }

    @Override
    public Page<PaymentLogDTO> getPaymentDtoList(PagingRequest pagingRequest) {
        FilterPaymentDTO filterPayment = pagingRequest.convertTo(FilterPaymentDTO.builder().build());
        org.springframework.data.domain.Page<Payment> payments = listLimitedByFilters(filterPayment, pagingRequest);
        return getPage(payments.getTotalElements(), PaymentLogDTO.convertToDto(payments.getContent()), pagingRequest);
    }

    @Override
    public List<PaymentLogDTO> getPaymentDtoExcelList(FilterPaymentDTO filter) {
        Specification<Payment> specification = getPaymentSpecification(filter);
        List<Payment> payments = listByFiltersForExcel(specification);
        return PaymentLogDTO.convertToDto(payments);
    }


    private List<Payment> listByFiltersForExcel(Specification<Payment> paymentSpecification) {
        Sort sort = Sort.by("id").descending();
        Pageable rows = PageRequest.of(0, 1000000, sort);
        if (paymentSpecification != null) {
            return paymentRepository.findAll(paymentSpecification, rows).toList();
        } else {
            return paymentRepository.findAll(rows).toList();
        }
    }

    @Override
    public List<Payment> findByTransactionAndProvider(String transaction, PaymentProvider paymentProvider) {
        return paymentRepository.findByTransactionAndProvider(transaction, paymentProvider);
    }

    @Override
    public List<Payment> findByTransaction(String transaction) {
        return paymentRepository.findByTransaction(transaction);
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public void cancelPaymentByTransactionId(String transaction, String reason) throws ResponseStatusException {
        List<Payment> payments = paymentRepository.findByTransaction(transaction);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Error : Entity not found by transactionId : " + transaction);
        }

        // Тут оставим костыль, т.к. по неведомой причине findByTransaction возвращает List<платежей>
        Payment firstPayment = payments.get(0);

        // Проверим была ли уже отозвана транзакция, если да то не имеет смысла откатывать повторно
        if (firstPayment.isCanceled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Error : Request already canceled by transactionId : " + transaction);
        }
        BigDecimal currentBalance = balanceService.getBalance(firstPayment.getCarNumber());
        if (currentBalance.compareTo(firstPayment.getPrice()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Error : Insufficient funds to cancel transaction : " + transaction +
                    " current balance : " + currentBalance);
        }
        paymentRepository.cancelPayment(transaction, reason);
        balanceService.subtractBalance(
                firstPayment.getCarNumber(),
                firstPayment.getPrice(),
                firstPayment.getCarStateId(),
                firstPayment.getDescription(),
                firstPayment.getDescription(),
                firstPayment.getProvider().getName());
    }

    private List<Payment> listByFilters(FilterPaymentDTO filterDto) {
        Specification<Payment> specification = getPaymentSpecification(filterDto);

        Sort sort = Sort.by("id").descending();

        return paymentRepository.findAll(specification, sort);
    }

    private org.springframework.data.domain.Page<Payment> listLimitedByFilters(FilterPaymentDTO filterDto, PagingRequest pagingRequest) {
        Specification<Payment> specification = getPaymentSpecification(filterDto);

        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData();
        Direction dir = order.getDir();

        Sort sort = null;
        if ("id".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("id").descending();
            } else {
                sort = Sort.by("id").ascending();
            }
        } else if ("parking".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("parking").descending();
            } else {
                sort = Sort.by("parking").ascending();
            }
        } else if ("carNumber".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("carNumber").descending();
            } else {
                sort = Sort.by("carNumber").ascending();
            }
        } else if ("inDate".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("inDate").descending();
            } else {
                sort = Sort.by("inDate").ascending();
            }
        } else if ("outDate".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("outDate").descending();
            } else {
                sort = Sort.by("outDate").ascending();
            }
        } else if ("created".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("created").descending();
            } else {
                sort = Sort.by("created").ascending();
            }
        } else if ("price".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("price").descending();
            } else {
                sort = Sort.by("price").ascending();
            }
        } else if ("rateDetails".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("rateDetails").descending();
            } else {
                sort = Sort.by("rateDetails").ascending();
            }
        } else if ("provider".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("provider").descending();
            } else {
                sort = Sort.by("provider").ascending();
            }
        } else if ("transaction".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("transaction").descending();
            } else {
                sort = Sort.by("transaction").ascending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);

        return paymentRepository.findAll(specification, rows);
    }

    private Specification<Payment> getPaymentSpecification(FilterPaymentDTO filterDto) {
        Specification<Payment> specification = null;

        if (!StringUtils.isEmpty(filterDto.getCarNumber())) {
            specification = PaymentSpecification.likePlateNumber(filterDto.getCarNumber());
        }
        if (filterDto.getDateFrom() != null) {
            specification = specification != null ? specification.and(PaymentSpecification.greaterDate(filterDto.getDateFrom())) : PaymentSpecification.greaterDate(filterDto.getDateFrom());
        }
        if (filterDto.getDateTo() != null) {
            specification = specification != null ? specification.and(PaymentSpecification.lessDate(filterDto.getDateTo())) : PaymentSpecification.lessDate(filterDto.getDateTo());
        }
        if (filterDto.getTotal() != null) {
            specification = specification != null ? specification.and(PaymentSpecification.equalAmount(filterDto.getTotal())) : PaymentSpecification.equalAmount(filterDto.getTotal());
        }
        if (filterDto.getPaymentProvider() != null) {
            specification = specification != null ? specification.and(PaymentSpecification.equalProvider(filterDto.getPaymentProvider())) : PaymentSpecification.equalProvider(filterDto.getPaymentProvider());
        }
        if (filterDto.getTransaction() != null) {
            specification = specification != null ? specification.and(PaymentSpecification.equalTransaction(filterDto.getTransaction())) : PaymentSpecification.equalTransaction(filterDto.getTransaction());
        }
        return specification;
    }

    private Page<PaymentLogDTO> getPage(long count, List<PaymentLogDTO> paymentLogDTOList, PagingRequest pagingRequest) {
        Page<PaymentLogDTO> page = new Page<>(paymentLogDTOList);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    public String toLog(PaymentCheckLog log) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        return "created: " + checkField(format.format(log.getCreated())) +
                ", plateNumber: " + checkField(log.getPlateNumber()) +
                ", message: " + checkField(log.getMessage()) +
                ", summ: " + checkField(log.getSumm()) +
                ", currentBalance: " + checkField(log.getCurrentBalance()) +
                ", carStateId: " + checkField(log.getCarStateId()) +
                ", paymentCheckType: " + checkField(log.getPaymentCheckType()) +
                ", transaction: " + checkField(log.getTransaction()) +
                ", providerName: " + checkField(log.getProviderName());
    }


    private <T> Object checkField(T s) {
        return ObjectUtils.isEmpty(s) ? "" : s;
    }

    public void sendingListOfPayment() {
        log.info("Payment_registry started !!!");
        FilterPaymentDTO filterPaymentDTO = new FilterPaymentDTO();
        LocalDate localDate = LocalDate.now();
        LocalDateTime startOfDay = localDate.minusDays(1).atStartOfDay();
        LocalDateTime endOfDay = localDate.atStartOfDay();
        filterPaymentDTO.setDateFrom(Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()));
        filterPaymentDTO.setDateTo(Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant()));
        List<PaymentLogDTO> paymentList = getPaymentDtoExcelList(filterPaymentDTO);
        XSSFWorkbook book = new XSSFWorkbook();
        try (book) {
            XSSFSheet main = book.createSheet("Payments_" + localDate.minusDays(1));
            int rowNum = 0;
            int colNum = 0;
            XSSFRow headerRow = main.createRow(rowNum++);
            for (HeaderDto header : paymentHeader()) {
                addCell(headerRow, header.getValue(), colNum++);
            }
            cellCreator(paymentList, rowNum,
                    main, paymentHeader());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                book.write(bos);
            } finally {
                bos.close();
            }
            byte[] excelFileAsBytes = bos.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(excelFileAsBytes);
            rootServicesGetterService.getMailService().sendEmailWithFile(
                    "Payments_" + localDate.minusDays(1) + ".xlsx", resource);
        } catch (IOException e) {
            log.error("[Error] while sending scheduled payment list");
        }
    }

    private List<HeaderDto> paymentHeader() {
        List<HeaderDto> list = new ArrayList<>();
        list.add(new HeaderDto("id", "№"));
        list.add(new HeaderDto("carNumber", "Номер авто"));
        list.add(new HeaderDto("parking", "Паркинг"));
        list.add(new HeaderDto("inDate", "Дата заезда"));
        list.add(new HeaderDto("outDate", "Дата выезда"));
        list.add(new HeaderDto("created", "Дата оплаты"));
        list.add(new HeaderDto("rateDetails", "Тариф"));
        list.add(new HeaderDto("price", "Сумма"));
        list.add(new HeaderDto("provider", "Провайдер"));
        list.add(new HeaderDto("transaction", "Транзакция"));
        return list;
    }

    private void cellCreator(List<?> list, int rowNum, XSSFSheet main, List<HeaderDto> headerList) {
        for (Object o : list) {
            int colNum = 0;
            XSSFRow row = main.createRow(rowNum++);
            try {
                for (HeaderDto headerDto : headerList) {
                    Field privateField
                            = list.get(0).getClass().getDeclaredField(headerDto.getKey());
                    privateField.setAccessible(true);
                    String value = String.valueOf(privateField.get(o));
                    if (ObjectUtils.isEmpty(value) || value.equals("null")) {
                        addCell(row, "", colNum++);
                    } else {
                        addCell(row, value, colNum++);
                    }
                }
            } catch (Exception e) {
                addCell(row, "", colNum++);
            }
        }
    }

    private void addCell(XSSFRow row, String value, int colNum) {
        XSSFCell cell = row.createCell(colNum);
        cell.setCellValue(value == null ? "" : value);
    }

    @Bean
    public void doStart() {
        startToSendPaymentList();
    }

    public void startToSendPaymentList() {
        log.info("Scheduled dispatch of payment started!");
        future = scheduler.schedule(this::sendingListOfPayment,
                triggerContext -> {
                    String cron = cronConfig();
                    if (cron != null) {
                        CronTrigger trigger = new CronTrigger(cron);
                        return trigger.nextExecutionTime(triggerContext);
                    }
                    return null;
                });
    }

    public void stopToSendPaymentList() {
        if (future != null) {
            future.cancel(false);
        }
        log.info("Scheduled dispatch of payment stopped!");
    }

    private String cronConfig() {
        Optional<Property> property = rootServicesGetterService.getPropertyRepository().findFirstByKey(CRON_PR);
        if (property.isPresent() && !BooleanUtils.toBoolean(property.get().getDisabled())) {
            log.info("cron value: {} ", property.get().getValue());
            return property.get().getValue();
        }
        return null;
    }

}

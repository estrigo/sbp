package kz.spt.billingplugin.service.impl;

import kz.spt.billingplugin.bootstrap.datatable.PaymentDtoComparators;
import kz.spt.billingplugin.dto.FilterPaymentDTO;
import kz.spt.billingplugin.dto.PaymentLogDTO;
import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import kz.spt.billingplugin.model.PaymentSpecification;
import kz.spt.billingplugin.repository.PaymentRepository;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.CarStateSpecification;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentRepository paymentRepository;

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
        List<Payment> allPayments = listByFilters(filterPayment);
        return getPage(PaymentLogDTO.convertToDto(allPayments), pagingRequest);
    }

    @Override
    public List<PaymentLogDTO> getPaymentDtoList(FilterPaymentDTO filter) throws ParseException {
        List<Payment> allPayments = listByFilters(filter);
        return PaymentLogDTO.convertToDto(allPayments);
    }

    @Override
    public List<Payment> findByTransactionAndProvider(String transaction, PaymentProvider paymentProvider) {
        return paymentRepository.findByTransactionAndProvider(transaction, paymentProvider);
    }

    @Override
    public List<Payment> findByTransaction(String transaction) {
        return paymentRepository.findByTransaction(transaction);
    }

    @SneakyThrows
    private List<Payment> listByFilters(FilterPaymentDTO filterDto) {
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

        specification = specification != null ? specification.and(PaymentSpecification.orderById()) : PaymentSpecification.orderById();
        return paymentRepository.findAll(specification);
    }

    private Page<PaymentLogDTO> getPage(List<PaymentLogDTO> paymentLogDTOList, PagingRequest pagingRequest) {
        List<PaymentLogDTO> filtered = paymentLogDTOList.stream()
                .sorted(sortPaymentLogDTO(pagingRequest))
                .filter(filterPaymentLogDTOs(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = paymentLogDTOList.stream()
                .filter(filterPaymentLogDTOs(pagingRequest))
                .count();

        Page<PaymentLogDTO> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<PaymentLogDTO> filterPaymentLogDTOs(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch().getValue())) {
            return paymentLogDTOs -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return paymentLogDTOs -> (paymentLogDTOs.getParking() != null && paymentLogDTOs.getParking().toLowerCase().contains(value.toLowerCase())
                || (paymentLogDTOs.getInDate() != null && paymentLogDTOs.getInDate().toString().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getOutDate() != null && paymentLogDTOs.getOutDate().toString().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getCreated() != null && paymentLogDTOs.getCreated().toString().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getRateDetails() != null && paymentLogDTOs.getRateDetails().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getPrice() != null && paymentLogDTOs.getPrice().toString().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getProvider() != null && paymentLogDTOs.getProvider().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getTransaction() != null && paymentLogDTOs.getTransaction().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getCarNumber() != null && paymentLogDTOs.getCarNumber().toLowerCase().contains(value.toLowerCase()))
                || (paymentLogDTOs.getCustomerDetail() != null && paymentLogDTOs.getCustomerDetail().toLowerCase().contains(value.toLowerCase()))
        );
    }

    private Comparator<PaymentLogDTO> sortPaymentLogDTO(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<PaymentLogDTO> comparator = PaymentDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}

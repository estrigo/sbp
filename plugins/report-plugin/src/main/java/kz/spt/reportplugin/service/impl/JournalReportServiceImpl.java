package kz.spt.reportplugin.service.impl;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.service.PaymentService;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.service.CarStateService;
import kz.spt.reportplugin.dto.JournalReportDto;
import kz.spt.reportplugin.service.ReportService;
import kz.spt.reportplugin.service.RootGetterService;
import kz.spt.reportplugin.utils.StreamExtensions;
import lombok.RequiredArgsConstructor;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalReportServiceImpl implements ReportService<JournalReportDto> {
    private final CarStateService carStateService;
    private final PaymentService paymentService;

    @Override
    public List<JournalReportDto> list() {
        var carStates = (List<CarState>) carStateService.listByFilters(CarStateFilterDto.builder().build());
        var payments = (List<Payment>) paymentService.listAllPayments();

        var filter = carStates.stream()
                .flatMap(m -> StreamExtensions.defaultIfEmpty(payments.stream().filter(p -> p.getCarStateId() == m.getId()), Payment::new)
                        .map(p -> JournalReportDto.builder()
                                .carStateId(m.getId())
                                .paymentId(p.getId())
                                .carNumber(m.getCarNumber())
                                .outTimestamp(m.getOutTimestamp())
                                .inTimestamp(m.getInTimestamp())
                                .parkingTypeCode(m.getParking().getParkingType().name())
                                .sum(p.getPrice())
                                .provider(p.getProvider() != null ? p.getProvider().getName() : "")
                                .build()))
                .collect(Collectors.toList());

        return filter;
    }
}

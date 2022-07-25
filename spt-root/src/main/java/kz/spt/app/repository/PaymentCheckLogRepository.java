package kz.spt.app.repository;

import kz.spt.lib.model.PaymentCheckLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface PaymentCheckLogRepository extends JpaRepository<PaymentCheckLog, Long> {

    @Query("from PaymentCheckLog pcl WHERE pcl.carStateId is not null and pcl.paymentCheckType = 'STANDARD' and pcl.plateNumber = ?1 and pcl.created >= ?2 order by pcl.id")
    List<PaymentCheckLog> findLastSuccessCheck(String plateNumber, Date date);

    List<PaymentCheckLog> findPaymentCheckLogByProviderId(Long providerId, Pageable pageable);
    List<PaymentCheckLog> findPaymentCheckLogByProviderId(Long providerId);
}

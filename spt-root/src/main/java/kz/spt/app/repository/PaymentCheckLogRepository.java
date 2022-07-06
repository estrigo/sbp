package kz.spt.app.repository;

import kz.spt.lib.model.PaymentCheckLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCheckLogRepository extends JpaRepository<PaymentCheckLog, Long> {

    @Query("from PaymentCheckLog pcl WHERE pcl.carStateId is not null and pcl.paymentCheckType = 'STANDARD' and pcl.plateNumber = ?1 order by pcl.id desc")
    Page<PaymentCheckLog> findLastSuccessCheck(String plateNumber, Pageable page);
}

package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("From Payment p LEFT JOIN FETCH p.parking")
    List<Payment> listAllPaymentsWithParkings();

    @Query("From Payment p LEFT JOIN FETCH p.provider where p.carStateId = ?1")
    List<Payment> getPaymentsByCarStateIdWithProvider(Long carStateId);

    @Query("From Payment p WHERE p.transaction = ?1 and p.provider = ?2")
    List<Payment> findByTransactionAndProvider(String transaction, PaymentProvider paymentProvider);
}

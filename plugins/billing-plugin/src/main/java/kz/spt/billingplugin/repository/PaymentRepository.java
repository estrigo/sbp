package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    @Query("From Payment p LEFT JOIN FETCH p.parking")
    List<Payment> listAllPaymentsWithParkings();

    @Query("From Payment p LEFT JOIN FETCH p.provider where p.carStateId = ?1")
    List<Payment> getPaymentsByCarStateIdWithProvider(Long carStateId);

    @Query("From Payment p WHERE p.transaction = ?1 and p.provider = ?2")
    List<Payment> findByTransactionAndProvider(String transaction, PaymentProvider paymentProvider);

    List<Payment> findByTransaction(String transaction);

    List<Payment> findAllByCreatedBetweenAndProviderName(
            Date dateFrom,
            Date dateTo,
            String providerName);

    Optional<Payment> findFirstByTransactionAndProviderNameAndCreated(
            String transactionId,
            String providerName,
            Date created);

    @Modifying
    @Query("UPDATE Payment p SET p.canceled = true, p.cancelReason = :reason where p.transaction = :transactionId")
    void cancelPayment(
            @Param(value = "transactionId") String transactionId,
            @Param(value = "reason") String reason);
}

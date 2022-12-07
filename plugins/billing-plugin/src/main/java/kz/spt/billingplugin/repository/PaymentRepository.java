package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.Payment;
import kz.spt.billingplugin.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    @Query("From Payment p LEFT JOIN FETCH p.parking")
    List<Payment> listAllPaymentsWithParkings();

    @Query("From Payment p LEFT JOIN FETCH p.provider where p.carStateId = ?1")
    List<Payment> getPaymentsByCarStateIdWithProvider(Long carStateId);

    @Query(value = "select * from payments p " +
            "LEFT JOIN payment_provider pp on p.provider_id = pp.id " +
            "where p.car_state_id in ?1", nativeQuery = true)
    List<Payment> getPaymentsByCarStateIdWithProvider(Collection<Long> carStateId);

    @Query("From Payment p WHERE p.transaction = ?1 and p.provider = ?2")
    List<Payment> findByTransactionAndProvider(String transaction, PaymentProvider paymentProvider);

    List<Payment> findByTransaction(String transaction);

    @Query(value = "SELECT * FROM payments p WHERE p.provider_id = (select id from payment_provider WHERE name =:providerName LIMIT 1) " +
            "AND (cast(p.created as Date) BETWEEN :dateFrom AND :dateTo)", nativeQuery = true)
    List<Payment> findAllByCreatedBetweenAndProviderName(
            @Param("providerName") String providerName,
            @Param("dateFrom") String dateFrom,
            @Param("dateTo") String dateTo);

    @Query(value = "SELECT * FROM payments p WHERE p.tnx_id =:transactionId " +
            "AND p.provider_id = (select id from payment_provider WHERE name =:providerName LIMIT 1) " +
            "AND cast(p.created as Date) =:created LIMIT 1", nativeQuery = true)
    Optional<Payment> findFirstByTransactionAndProviderNameAndCreated(
            @Param("transactionId") String transactionId,
            @Param("providerName") String providerName,
            @Param("created") String created);

    @Modifying
    @Query("UPDATE Payment p SET p.canceled = true, p.cancelReason = :reason where p.transaction = :transactionId")
    void cancelPayment(
            @Param(value = "transactionId") String transactionId,
            @Param(value = "reason") String reason);

    List<Payment> findAllByCreatedAfterAndProviderInAndCheckNumberIsNull(
            Date date, List<PaymentProvider> providers);

}

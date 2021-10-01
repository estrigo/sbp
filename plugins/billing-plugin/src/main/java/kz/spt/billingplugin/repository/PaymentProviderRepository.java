package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {

    @Query("from PaymentProvider p where p.id = ?1")
    PaymentProvider getPaymentProvider(Long id);

    @Query("from PaymentProvider p where p.clientId = ?1")
    PaymentProvider findByClientId(String clienId);
}

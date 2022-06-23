package kz.spt.prkstatusplugin.repository;


import kz.spt.prkstatusplugin.model.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, Long> {

    @Query("from PaymentProvider p where p.id = ?1")
    PaymentProvider getPaymentProvider(Long id);

    @Query("from PaymentProvider p where p.clientId = ?1")
    PaymentProvider findByClientId(String clienId);

    @Query("from PaymentProvider p where p.name = ?1")
    PaymentProvider findByName(String name);

    @Query("from PaymentProvider p where p.isParkomat = true")
    List<PaymentProvider> getParkomatProviders();
}

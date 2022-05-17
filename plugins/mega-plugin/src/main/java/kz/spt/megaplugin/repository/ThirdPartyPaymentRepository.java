package kz.spt.megaplugin.repository;

import kz.spt.megaplugin.model.ThirdPartyPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("thirdPartyPaymentRepository")
public interface ThirdPartyPaymentRepository extends JpaRepository<ThirdPartyPayment, Long> {

    @Query("from ThirdPartyPayment th WHERE th.sent=false")
    List<ThirdPartyPayment> findNotSentThirdPartyPayments();

}

package kz.spt.megaplugin.repository;

import kz.spt.megaplugin.model.ThirdPartyPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("thirdPartyPaymentRepository")
public interface ThirdPartyPaymentRepository extends JpaRepository<ThirdPartyPayment, Long> {

}

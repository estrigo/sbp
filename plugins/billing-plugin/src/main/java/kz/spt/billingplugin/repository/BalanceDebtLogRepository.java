package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.BalanceDebtLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceDebtLogRepository extends JpaRepository<BalanceDebtLog, Long> {

}

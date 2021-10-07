package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRepository  extends JpaRepository<Balance, String> {
}

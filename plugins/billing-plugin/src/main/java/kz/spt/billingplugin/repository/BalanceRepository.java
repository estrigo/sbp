package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BalanceRepository  extends JpaRepository<Balance, String> {

    @Query("from Balance b where b.balance < 0")
    List<Balance> debtBalances();

    @Query("from Balance b where b.plateNumber = ?1")
    Balance getBalanceByPlateNumber(String plateNumber);
}

package kz.spt.billingplugin.repository;

import kz.spt.billingplugin.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("from Transaction t where t.car.platenumber = ?1 and t.date >= ?2")
    List<Transaction> findAllByPlateNumberAndDateAfter(String plateNumber, Date date);

}

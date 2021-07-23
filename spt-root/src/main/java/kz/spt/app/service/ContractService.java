package kz.spt.app.service;

import kz.spt.app.entity.Contract;
import kz.spt.app.entity.Customer;
import kz.spt.app.entity.Status;
import kz.spt.api.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ContractService {

    Contract findByName(String contractName);

    Iterable<Contract> listAllContracts();

    Contract showContract(Long id);

    Iterable<Contract> findAllByValueLessThanEqual(BigDecimal value);

    Iterable<Contract> findAllByValueGreaterThanEqual(BigDecimal value);

    Iterable<Contract> findAllByBeginDate(LocalDate beginDate);

    Iterable<Contract> findAllByBeginDateBefore(LocalDate beforeBeginDate);

    Iterable<Contract> findAllByBeginDateAfter(LocalDate afterBeginDate);

    Iterable<Contract> findAllByEndDate(LocalDate endDate);

    Iterable<Contract> findAllByEndDateBefore(LocalDate beforeEndDate);

    Iterable<Contract> findAllByEndDateAfter(LocalDate afterEndDate);

    Iterable<Contract> findAllByStatus(Status status);

    Iterable<Contract> findAllByCustomer(Customer customer);

    Iterable<Contract> findAllByCustomerAndUser(Customer customer, User user);

    Iterable<Contract> findAllByUser(User user);

    void saveContract(Contract contract);

}

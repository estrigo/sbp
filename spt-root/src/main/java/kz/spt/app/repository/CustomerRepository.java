package kz.spt.app.repository;

import kz.spt.lib.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>  {
    @Query("from Customer c left join fetch c.cars where c.id = ?1")
    Customer getCustomerWithCar(Long id);

    @Query("SELECT c from Customer c where c.phoneNumber=?1")
    List<Customer> getCustomerIfAnyExist(String phoneNumber);

}

package kz.spt.app.repository;

import kz.spt.lib.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>  {
    @Query("from Customer c left join fetch c.cars where c.id = ?1")
    Customer getCustomerWithCar(Long id);

}

package kz.spt.bookingplugin.repository;

import kz.spt.bookingplugin.model.BookingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<BookingLog, Long> {

}

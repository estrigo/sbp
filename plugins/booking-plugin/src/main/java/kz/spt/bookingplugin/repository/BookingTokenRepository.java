package kz.spt.bookingplugin.repository;

import kz.spt.bookingplugin.model.BookingToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingTokenRepository extends JpaRepository<BookingToken, Long> {

    BookingToken findByClient (BookingToken.Client client);
}

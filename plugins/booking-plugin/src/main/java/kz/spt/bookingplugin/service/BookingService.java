package kz.spt.bookingplugin.service;

import java.io.IOException;
import java.net.URISyntaxException;

public interface BookingService {

    Boolean checkBookingValid(String plateNumber, String position) throws IOException, URISyntaxException;
}

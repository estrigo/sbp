package kz.spt.bookingplugin.service;

import kz.spt.bookingplugin.exceptions.BookingValidException;

import java.io.IOException;
import java.net.URISyntaxException;

public interface BookingService {

    Boolean checkBookingValid(String plateNumber, String region, String position, String entrance) throws BookingValidException;
}

package kz.spt.api.service;

import kz.spt.api.model.Camera;
import kz.spt.api.model.CarState;

import java.util.Date;

public interface CarStateService {

    void createINState(String carNumber, Date inTimestamp, Camera camera);

    void createOUTState(String carNumber, Date outTimestamp, Camera camera, Long paymentId, Long amount, Boolean paid);

    Boolean checkIsLastEnteredNotLeft(String carNumber);

    CarState getLastNotLeft(String carNumber);

    Iterable<CarState> getAllNotLeft();

}

package kz.spt.lib.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;

import java.util.Date;

public interface CarStateService {

    void createINState(String carNumber, Date inTimestamp, Camera camera);

    void createOUTState(String carNumber, Date outTimestamp, Camera camera, Long paymentId, Long amount, Boolean paid);

    Boolean checkIsLastEnteredNotLeft(String carNumber);

    CarState getLastNotLeft(String carNumber);

    Iterable<CarState> getAllNotLeft();

    Page<CarStateDto> getAll(PagingRequest pagingRequest, String plateNumber, String dateFromString, String dateToString);

}

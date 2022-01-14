package kz.spt.lib.service;

import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateFilterDto;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface CarStateService {

    void createINState(String carNumber, Date inTimestamp, Camera camera, Boolean paid, String whitelistJson);

    void createOUTState(String carNumber, Date outTimestamp, Camera camera, CarState carState);

    void createOUTManual(String carNumber, Date outTimestamp, CarState carState);

    Boolean checkIsLastEnteredNotLeft(String carNumber);

    CarState getLastNotLeft(String carNumber);

    Iterable<CarState> getAllNotLeft();

    Page<CarStateDto> getAll(PagingRequest pagingRequest,
                             CarStateFilterDto carStateFilterDto) throws ParseException;

    CarState save(CarState carState);

    List<String> getInButNotPaidFromList(List<String> checkList);

    Boolean checkIsLastLeft(String carNumber, String cameraIp);

    Boolean removeDebt(String carNumber);
}

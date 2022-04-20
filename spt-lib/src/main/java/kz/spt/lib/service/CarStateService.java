package kz.spt.lib.service;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import kz.spt.lib.model.Camera;
import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateExcelDto;
import kz.spt.lib.model.dto.CarStateFilterDto;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface CarStateService {

    CarState findById(Long carStateId);

    void createINState(String carNumber, Date inTimestamp, Camera camera, Boolean paid, String whitelistJson, String inPhotoUrl);

    void createOUTState(String carNumber, Date outTimestamp, Camera camera, CarState carState, String outPhotoUrl);

    void createOUTManual(String carNumber, Date outTimestamp, CarState carState);

    CarState getLastNotLeft(String carNumber);

    Iterable<CarState> getAllNotLeft();

    void editPlateNumber(CarState carState);

    Iterable<CarState> listByFilters(CarStateFilterDto filterDto);

    Page<CarStateDto> getAll(PagingRequest pagingRequest,
                             CarStateFilterDto carStateFilterDto) throws ParseException;

    CarState save(CarState carState);

    List<String> getInButNotPaidFromList(List<String> checkList);

    CarState getIfLastLeft(String carNumber, String cameraIp);

    Boolean getIfHasLastFromOtherCamera(String carNumber, String cameraIp, Date secondsBefore);

    Boolean getIfHasLastFromThisCamera(String carNumber, String cameraIp, Date secondsBefore);

    Boolean removeDebt(String carNumber) throws Exception;

    Iterable<CarState> getCurrentNotPayed(String carNumber);

    CarState getLastCarState(String carNumber);

    void setAbonomentDetails(Long id, JsonNode details);

    List<CarStateExcelDto> getExcelData(CarStateFilterDto carStateFilterDto) throws ParseException;
}

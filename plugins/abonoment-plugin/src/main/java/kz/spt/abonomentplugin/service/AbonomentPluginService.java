package kz.spt.abonomentplugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.abonomentplugin.dto.AbonomentDTO;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.model.Abonement;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.abonomentplugin.model.dto.AbonementFilterDto;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

public interface AbonomentPluginService {

    AbonomentTypes createType(int period,String customJson, String type, int price) throws JsonProcessingException;

    void deleteType(Long id);

    Page<AbonomentTypeDTO> abonomentTypeDtoList(PagingRequest pagingRequest);

    List<AbonomentTypes> getAllAbonomentTypes();

    Abonement createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws ParseException;

    void deleteAbonoment(Long id);

    void deleteAbonomentByParkingID(Long parkingId);

    Page<AbonomentDTO> abonomentDtoList(PagingRequest pagingRequest, AbonementFilterDto filter) throws ParseException;

    JsonNode getUnpaidNotExpiredAbonoment(String plateNumber);

    void setAbonomentPaid(Long id);

    JsonNode getPaidNotExpiredAbonoment(String plateNumber, Long parkingId, Date carInDate) throws JsonProcessingException;

    Boolean checkAbonomentIntersection(String platenumber, Long parkingId, Long typeId, String dateStart, Boolean checked) throws ParseException;

    void deleteNotPaidExpired();

    void creteNewForOld();

    String checkExpiration(String plateNumber, Long parkingId);
}

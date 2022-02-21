package kz.spt.abonomentplugin.service;

import com.fasterxml.jackson.databind.JsonNode;
import kz.spt.abonomentplugin.dto.AbonomentDTO;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.model.Abonoment;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;

import java.text.ParseException;
import java.util.List;

public interface AbonomentPluginService {

    AbonomentTypes createType(int period, int price);

    void deleteType(Long id);

    Page<AbonomentTypeDTO> abonomentTypeDtoList(PagingRequest pagingRequest);

    List<AbonomentTypes> getAllAbonomentTypes();

    Abonoment createAbonoment(String platenumber, Long parkingId, Long typeId, String dateStart) throws ParseException;

    void deleteAbonoment(Long id);

    Page<AbonomentDTO> abonomentDtoList(PagingRequest pagingRequest);

    JsonNode getUnpaidNotExpiredAbonoment(String plateNumber);

    void setAbonomentPaid(Long id);
}

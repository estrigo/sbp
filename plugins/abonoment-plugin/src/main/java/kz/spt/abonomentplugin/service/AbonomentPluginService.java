package kz.spt.abonomentplugin.service;

import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;

public interface AbonomentPluginService {

    AbonomentTypes createType(int period, int price);

    void deleteType(Long id);

    Page<AbonomentTypeDTO> abonomentTypeDtoList(PagingRequest pagingRequest);
}

package kz.spt.abonomentplugin.service.impl;

import kz.spt.abonomentplugin.bootstrap.datatable.AbonomentTypeDtoComparators;
import kz.spt.abonomentplugin.dto.AbonomentTypeDTO;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.abonomentplugin.repository.AbonomentTypesRepository;
import kz.spt.abonomentplugin.service.AbonomentPluginService;
import kz.spt.lib.bootstrap.datatable.Column;
import kz.spt.lib.bootstrap.datatable.Order;
import kz.spt.lib.bootstrap.datatable.Page;
import kz.spt.lib.bootstrap.datatable.PagingRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class AbonomentPluginServiceImpl implements AbonomentPluginService {

    private final AbonomentTypesRepository abonomentTypesRepository;

    private static final Comparator<AbonomentTypeDTO> EMPTY_COMPARATOR = (e1, e2) -> 0;

    public AbonomentPluginServiceImpl(AbonomentTypesRepository abonomentTypesRepository){
        this.abonomentTypesRepository = abonomentTypesRepository;
    }

    @Override
    public AbonomentTypes createType(int period, int price) {
        AbonomentTypes abonomentTypes = new AbonomentTypes();
        abonomentTypes.setPeriod(period);
        abonomentTypes.setPrice(price);
        AbonomentTypes savedAbonomentTypes = abonomentTypesRepository.save(abonomentTypes);
        return savedAbonomentTypes;
    }

    @Override
    public void deleteType(Long id){
        abonomentTypesRepository.deleteById(id);
    }

    @Override
    public Page<AbonomentTypeDTO> abonomentTypeDtoList(PagingRequest pagingRequest) {
        List<AbonomentTypes> allAbonomentTypes = listByFilters();
        return getPage(AbonomentTypeDTO.convertToDto(allAbonomentTypes), pagingRequest);
    }

    private List<AbonomentTypes> listByFilters() {
        return abonomentTypesRepository.findAll();
    }

    private Page<AbonomentTypeDTO> getPage(List<AbonomentTypeDTO> paymentLogDTOList, PagingRequest pagingRequest) {
        List<AbonomentTypeDTO> filtered = paymentLogDTOList.stream()
                .sorted(sortAbonomentTypeDTO(pagingRequest))
                .filter(filterAbonomentTypeDTOs(pagingRequest))
                .skip(pagingRequest.getStart())
                .limit(pagingRequest.getLength())
                .collect(Collectors.toList());

        long count = paymentLogDTOList.stream()
                .filter(filterAbonomentTypeDTOs(pagingRequest))
                .count();

        Page<AbonomentTypeDTO> page = new Page<>(filtered);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    private Predicate<AbonomentTypeDTO> filterAbonomentTypeDTOs(PagingRequest pagingRequest) {
        if (pagingRequest.getSearch() == null || StringUtils.isEmpty(pagingRequest.getSearch().getValue())) {
            return abonomentTypeDTOs -> true;
        }
        String value = pagingRequest.getSearch().getValue();

        return abonomentTypeDTOs -> (String.valueOf(abonomentTypeDTOs.period).contains(value) || String.valueOf(abonomentTypeDTOs.price).contains(value));
    }

    private Comparator<AbonomentTypeDTO> sortAbonomentTypeDTO(PagingRequest pagingRequest) {
        if (pagingRequest.getOrder() == null) {
            return EMPTY_COMPARATOR;
        }

        try {
            Order order = pagingRequest.getOrder().get(0);

            int columnIndex = order.getColumn();
            Column column = pagingRequest.getColumns().get(columnIndex);

            Comparator<AbonomentTypeDTO> comparator = AbonomentTypeDtoComparators.getComparator(column.getData(), order.getDir());
            return Objects.requireNonNullElse(comparator, EMPTY_COMPARATOR);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return EMPTY_COMPARATOR;
    }
}

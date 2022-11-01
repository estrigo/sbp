package kz.spt.app.service.impl;

import kz.spt.app.repository.BlacklistRepository;
import kz.spt.lib.bootstrap.datatable.*;
import kz.spt.lib.model.BlacklistSpecification;
import kz.spt.lib.model.Blacklist;
import kz.spt.lib.model.dto.BlacklistDto;
import kz.spt.lib.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(noRollbackFor = Exception.class)
public class BlacklistServiceImpl implements BlacklistService {
    private final BlacklistRepository blacklistRepository;

    @Override
    public Optional<Blacklist> findByPlate(String plateNumber) {
        return blacklistRepository.findByPlateNumber(plateNumber);
    }

    @Override
    public Page<BlacklistDto> getAll(PagingRequest pagingRequest) {
        String plateNumber = pagingRequest.getSearch().getValue();
        Specification<Blacklist> specification = getBlacklistSpecification(plateNumber);
        org.springframework.data.domain.Page<Blacklist> filteredBlackLists = listByFilters(specification, pagingRequest);
        return getPage(filteredBlackLists, pagingRequest);
    }

    private org.springframework.data.domain.Page<Blacklist> listByFilters(Specification<Blacklist> blacklistSpecification, PagingRequest pagingRequest) {
        Order order = pagingRequest.getOrder().get(0);

        int columnIndex = order.getColumn();
        Column column = pagingRequest.getColumns().get(columnIndex);
        String columnName = column.getData();
        Direction dir = order.getDir();

        Sort sort = null;
        if ("plateNumber".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("plateNumber").descending();
            } else {
                sort = Sort.by("plateNumber").ascending();
            }
        } else if ("type".equals(columnName)) {
            if (Direction.desc.equals(dir)) {
                sort = Sort.by("type").descending();
            } else {
                sort = Sort.by("type").ascending();
            }
        } else {
            if (Direction.asc.equals(dir)) {
                sort = Sort.by("id").ascending();
            } else {
                sort = Sort.by("id").descending();
            }
        }

        Pageable rows = PageRequest.of(pagingRequest.getStart() / pagingRequest.getLength(), pagingRequest.getLength(), sort);
        if (blacklistSpecification != null) {
            return blacklistRepository.findAll(blacklistSpecification, rows);
        } else {
            return blacklistRepository.findAll(rows);
        }
    }

    private Page<BlacklistDto> getPage(org.springframework.data.domain.Page<Blacklist> blacklists, PagingRequest pagingRequest) {
        long count = blacklists.getTotalElements();

        List<BlacklistDto> blacklistDtoList = BlacklistDto.fromBlacklists(blacklists.toList());

        Page<BlacklistDto> page = new Page<>(blacklistDtoList);
        page.setRecordsFiltered((int) count);
        page.setRecordsTotal((int) count);
        page.setDraw(pagingRequest.getDraw());

        return page;
    }

    @Override
    public void save(BlacklistDto model) {
        Blacklist blacklist = blacklistRepository.findByPlateNumber(model.getPlateNumber())
                .map(m-> {
                    m.setPlateNumber(model.getPlateNumber());
                    m.setType(model.getType());
                    return m;
                })
                .orElse(Blacklist.builder()
                        .plateNumber(model.getPlateNumber())
                        .type(model.getType())
                        .build());
        blacklistRepository.save(blacklist);
    }

    @Override
    public void delete(Long id) {
        blacklistRepository.deleteById(id);
    }

    private Specification<Blacklist> getBlacklistSpecification(String plateNumber) {
        Specification<Blacklist> specification = null;

        if (!StringUtils.isEmpty(plateNumber)) {
            specification = BlacklistSpecification.likeNumber(plateNumber);
        }
        return specification;
    }
}

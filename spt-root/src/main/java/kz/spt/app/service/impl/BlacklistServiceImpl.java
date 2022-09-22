package kz.spt.app.service.impl;

import kz.spt.app.repository.BlacklistRepository;
import kz.spt.app.service.BlacklistService;
import kz.spt.lib.model.Blacklist;
import kz.spt.lib.model.dto.BlacklistDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistServiceImpl implements BlacklistService {
    private final BlacklistRepository blacklistRepository;

    @Override
    public Optional<Blacklist> findByPlate(String plateNumber) {
        return blacklistRepository.findByPlateNumber(plateNumber);
    }

    @Override
    public List<BlacklistDto> list() {
        return blacklistRepository.findAll().stream()
                .map(m->BlacklistDto.builder()
                        .id(m.getId())
                        .plateNumber(m.getPlateNumber())
                        .type(m.getType())
                        .build())
                .collect(Collectors.toList());
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
}

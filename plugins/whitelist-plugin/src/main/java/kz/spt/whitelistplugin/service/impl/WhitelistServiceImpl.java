package kz.spt.whitelistplugin.service.impl;

import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.WhitelistRepository;
import kz.spt.whitelistplugin.service.WhitelistService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WhitelistServiceImpl implements WhitelistService {

    private WhitelistRepository whitelistRepository;

    public WhitelistServiceImpl(WhitelistRepository whitelistRepository){
        this.whitelistRepository = whitelistRepository;
    }

    @Override
    public void saveWhitelist(Whitelist whitelist) {

    }

    @Override
    public List<Whitelist> listAllWhitelist() {
        return whitelistRepository.findAll();
    }
}

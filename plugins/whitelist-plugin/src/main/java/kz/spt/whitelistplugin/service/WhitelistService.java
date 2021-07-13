package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.Whitelist;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist) throws Exception;

    Iterable<Whitelist> listAllWhitelist();

}
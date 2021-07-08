package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.Whitelist;

import java.util.List;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist);

    List<Whitelist> listAllWhitelist();

}
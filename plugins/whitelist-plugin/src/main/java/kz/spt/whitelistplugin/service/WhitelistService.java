package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.Whitelist;

import java.util.Date;

public interface WhitelistService {

    void saveWhitelist(Whitelist whitelist) throws Exception;

    Iterable<Whitelist> listAllWhitelist();

    public Boolean hasAccess(String platenumber, Date enterDate);
}
package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.WhitelistCategory;

public interface WhitelistCategoryService {

    WhitelistCategory getById(Long id);

    Iterable<WhitelistCategory> listAllCategories();

    WhitelistCategory save(WhitelistCategory category, String username);
}

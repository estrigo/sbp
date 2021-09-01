package kz.spt.whitelistplugin.service;

import kz.spt.whitelistplugin.model.Category;

public interface CategoryService {

    Category getById(Long id);

    Iterable<Category> listAllCategories();
}

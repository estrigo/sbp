package kz.spt.whitelistplugin.service.impl;

import kz.spt.whitelistplugin.model.WhitelistCategory;
import kz.spt.whitelistplugin.repository.WhitelistCategoryRepository;
import kz.spt.whitelistplugin.service.WhitelistCategoryService;
import org.springframework.stereotype.Service;

@Service
public class WhitelistCategoryServiceImpl implements WhitelistCategoryService {

    private WhitelistCategoryRepository whitelistCategoryRepository;

    public WhitelistCategoryServiceImpl(WhitelistCategoryRepository whitelistCategoryRepository){
        this.whitelistCategoryRepository = whitelistCategoryRepository;
    }

    @Override
    public WhitelistCategory getById(Long id) {
        return whitelistCategoryRepository.getById(id);
    }

    @Override
    public Iterable<WhitelistCategory> listAllCategories() {
        return whitelistCategoryRepository.findAll();
    }

    @Override
    public WhitelistCategory save(WhitelistCategory category, String username) {
        category.setUpdatedUser(username);
        return whitelistCategoryRepository.save(category);
    }
}

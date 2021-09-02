package kz.spt.whitelistplugin.service.impl;

import kz.spt.whitelistplugin.model.Category;
import kz.spt.whitelistplugin.repository.CategoryRepository;
import kz.spt.whitelistplugin.service.CategoryService;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl implements CategoryService {

    private CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category getById(Long id) {
        return categoryRepository.getOne(id);
    }

    @Override
    public Iterable<Category> listAllCategories() {
        return categoryRepository.findAll();
    }
}

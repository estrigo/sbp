package kz.spt.whitelistplugin.controller;

import kz.spt.whitelistplugin.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/whitelist/category")
public class CategoryController {

    private CategoryService categoryService;

    public CategoryController(CategoryService categoryService){
        this.categoryService = categoryService;
    }

    @GetMapping("/list")
    public String showAllCategory(Model model) {
        model.addAttribute("categories", categoryService.listAllCategories());
        return "/whitelist/category/list";
    }
}

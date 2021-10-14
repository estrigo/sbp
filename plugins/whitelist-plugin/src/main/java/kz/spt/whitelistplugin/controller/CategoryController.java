package kz.spt.whitelistplugin.controller;

import kz.spt.lib.model.Barrier;
import kz.spt.lib.model.Gate;
import kz.spt.whitelistplugin.model.WhitelistCategory;
import kz.spt.whitelistplugin.service.WhitelistCategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/whitelist/category")
public class CategoryController {

    private WhitelistCategoryService categoryService;

    public CategoryController(WhitelistCategoryService categoryService){
        this.categoryService = categoryService;
    }

    @GetMapping("/add")
    public String showFormAddGroup(Model model) {
        model.addAttribute("category", new WhitelistCategory());
        return "/whitelist/category/add";
    }

    @PostMapping("/add")
    public String processRequestAddGroup(Model model, @Valid WhitelistCategory category, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser) throws Exception {
        if (bindingResult.hasErrors()) {
            return "whitelist/add";
        } else {
            if(category.getName() == null || "".equals(category.getName())){
                ObjectError error = new ObjectError("emptyName", "Please fill name");
                bindingResult.addError(error);
            }

            if(!bindingResult.hasErrors()){
                categoryService.save(category, currentUser.getUsername());
                return "redirect:/whitelist/list";
            }
            return "whitelist/groups/add";
        }
    }

    @GetMapping("/edit/{categoryId}")
    public String getEditingCategoryId(Model model, @PathVariable Long categoryId) {
        model.addAttribute("category", categoryService.getById(categoryId));
        return "whitelist/category/edit";
    }

    @PostMapping("/edit/{categoryId}")
    public String categoryEdit(@PathVariable Long categoryId, @Valid WhitelistCategory category, BindingResult bindingResult, @AuthenticationPrincipal UserDetails currentUser){

        if (!bindingResult.hasErrors()) {
            categoryService.save(category, currentUser.getUsername());
        }
        return "redirect:/whitelist/list";
    }
}

package kz.spt.app.controller;

import kz.spt.app.service.RoleService;
import kz.spt.lib.model.User;
import kz.spt.lib.service.UserService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.thymeleaf.util.StringUtils;

import javax.validation.Valid;
import java.util.Locale;
import java.util.ResourceBundle;

@Controller
public class RegisterController {

    private UserService userService;
    private RoleService roleService;

    public RegisterController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model){
        model.addAttribute("user", new User());
        model.addAttribute("allRoles", roleService.listRolesByPlugins());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistrationForm(Model model, @Valid User user, BindingResult bindingResult) {
        Locale locale = LocaleContextHolder.getLocale();
        String language = locale.getLanguage();

        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));

        if(StringUtils.isEmpty(user.getUsername())){
            ObjectError error = new ObjectError("usernameIsNull", bundle.getString("user.usernameIsNull"));
            bindingResult.addError(error);
        } else {
            User userFromDB = userService.findByUsername(user.getUsername());
            if (userFromDB != null) {
                ObjectError error = new ObjectError("alreadyRegisteredMessage", bundle.getString("user.alreadyRegisteredMessage"));
                bindingResult.addError(error);
            }
        }
        if(StringUtils.isEmpty(user.getPassword())){
            ObjectError error = new ObjectError("passwordIsNull", bundle.getString("user.emailIsNull"));
            bindingResult.addError(error);
        }
        if(StringUtils.isEmpty(user.getFirstName())){
            ObjectError error = new ObjectError("firstnameIsNull", bundle.getString("user.firstNameIsNull"));
            bindingResult.addError(error);
        }
        if(StringUtils.isEmpty(user.getLastName())){
            ObjectError error = new ObjectError("lastnameIsNull", bundle.getString("user.lastNameIsNull"));
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.listRolesByPlugins());
            return "register";
        } else {
            userService.saveUser(user);
            return "redirect:users/list";
        }
    }

}

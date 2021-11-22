package kz.spt.app.controller;

import kz.spt.app.service.RoleService;
import kz.spt.lib.model.User;
import kz.spt.lib.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.thymeleaf.util.StringUtils;

import javax.validation.Valid;

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
        if(StringUtils.isEmpty(user.getUsername())){
            ObjectError error = new ObjectError("usernameIsNull", "Пользователь не может быть пустым");
            bindingResult.addError(error);
        } else {
            User userFromDB = userService.findByUsername(user.getUsername());
            if (userFromDB != null) {
                ObjectError error = new ObjectError("alreadyRegisteredMessage", "В системе уже существует пользователь");
                bindingResult.addError(error);
            }
        }
        if(StringUtils.isEmpty(user.getPassword())){
            ObjectError error = new ObjectError("passwordIsNull", "Email не может быть пустым");
            bindingResult.addError(error);
        }
        if(StringUtils.isEmpty(user.getFirstName())){
            ObjectError error = new ObjectError("firstnameIsNull", "Имя не может быть пустым");
            bindingResult.addError(error);
        }
        if(StringUtils.isEmpty(user.getLastName())){
            ObjectError error = new ObjectError("lastnameIsNull", "Фамилия не может быть пустым");
            bindingResult.addError(error);
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", roleService.listRolesByPlugins());
            return "register";
        } else {
            userService.saveUser(user);
            return "redirect:user/list";
        }
    }

}

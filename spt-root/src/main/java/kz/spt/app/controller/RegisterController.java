package kz.spt.app.controller;

import kz.spt.app.service.RoleService;
import kz.spt.lib.model.User;
import kz.spt.app.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

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
    public String showRegistrationPage(Model model, User user){
        model.addAttribute("user", user);
        model.addAttribute("allRoles", roleService.listRolesByPlugins());
        return "register";
    }

    @PostMapping("/register")
    public String processRegistrationForm(Model model, @Valid User user, BindingResult bindingResult) {
        User userFromDB = userService.findByUsername(user.getUsername());

        if (userFromDB != null) {
            model.addAttribute("alreadyRegisteredMessage",
                    "Oops!  There is already a user registered with the email provided.");
            bindingResult.reject("email");
            return "register";
        }

        if (bindingResult.hasErrors()) {
            return "redirect:/register";
        } else {
            userService.saveUser(user);
            return "redirect:user/list";
        }
    }

}

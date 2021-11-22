package kz.spt.app.controller;

import kz.spt.lib.model.User;
import kz.spt.app.service.RoleService;
import kz.spt.lib.service.UserService;
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
import org.thymeleaf.util.StringUtils;

import javax.validation.Valid;

@Controller
@RequestMapping("/users")
public class UserController {

    private UserService userService;
    private RoleService roleService;

    public UserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    /**
     * /users/list
     * <p>
     * Shows all users
     *
     * @param model model to attributes to
     * @return users/list
     */
    @GetMapping("/list")
    public String showAllUsers(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("currentUser", userService.findByUsername(currentUser.getUsername()));
        model.addAttribute("users", userService.listAllUsers());
        return "users/list";
    }

    /**
     * /users/edit/{id}
     * <p>
     * Shows edit user form
     *
     * @param model model to attributes to
     * @param id    variable type long user id
     * @return users/edit
     */
    @GetMapping("/edit/{id}")
    public String showFormEditUser(Model model, @PathVariable Long id) {
        model.addAttribute("user", userService.showUser(id));
        model.addAttribute("allRoles", roleService.listRolesByPlugins());
        return "users/edit";
    }

    /**
     * /users/edit/{id}
     * <p>
     * Processes edit user request
     *
     * @param id            variable type long user id
     * @param user          variable type User
     * @param bindingResult variable type BindingResult
     * @return redirect:/users/list
     */
    @PostMapping("/edit/{id}")
    public String processRequestEditUser(Model model, @PathVariable Long id, @Valid User user,
                                         BindingResult bindingResult) {
        if(StringUtils.isEmpty(user.getUsername())){
            ObjectError error = new ObjectError("usernameIsNull", "Пользователь не может быть пустым");
            bindingResult.addError(error);
        } else {
            User userFromDB = userService.findByUsername(user.getUsername());
            if (userFromDB != null && !userFromDB.getId().equals(user.getId())) {
                ObjectError error = new ObjectError("alreadyRegisteredMessage", "В системе уже существует пользователь");
                bindingResult.addError(error);
            }
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
            return "users/edit";
        } else {
            userService.editUser(user);
            return "redirect:/users/list";
        }
    }

    /**
     * /users/delete/{id}
     * <p>
     * Deletes user
     *
     * @param id variable type long user id
     * @return redirect:/users/list
     */
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(userService.showUser(id));
        return "redirect:/users/list";
    }

}

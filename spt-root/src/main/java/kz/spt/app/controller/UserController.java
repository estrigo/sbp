package kz.spt.app.controller;

import kz.spt.lib.model.User;
import kz.spt.app.service.RoleService;
import kz.spt.app.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;

@Controller
@RequestMapping("/user")
public class UserController {

    private UserService userService;

    private RoleService roleService;

    public UserController(UserService userService, RoleService roleService) {
        this.userService = userService;
        this.roleService = roleService;
    }

    /**
     * /user/list
     * <p>
     * Shows all users
     *
     * @param model model to attributes to
     * @return user/list
     */
    @GetMapping("/list")
    public String showAllUsers(Model model, @AuthenticationPrincipal UserDetails currentUser) {
        model.addAttribute("currentUser", userService.findByUsername(currentUser.getUsername()));
        model.addAttribute("users", userService.listAllUsers());
        return "user/list";
    }

    /**
     * /user/edit/{id}
     * <p>
     * Shows edit user form
     *
     * @param model model to attributes to
     * @param id    variable type long user id
     * @return user/edit
     */
    @GetMapping("/edit/{id}")
    public String showFormEditUser(Model model, @PathVariable Long id) {
        model.addAttribute("user", userService.showUser(id));
        model.addAttribute("allRoles", roleService.listRolesByPlugins());
        return "user/edit";
    }

    /**
     * /user/edit/{id}
     * <p>
     * Processes edit user request
     *
     * @param id            variable type long user id
     * @param user          variable type User
     * @param bindingResult variable type BindingResult
     * @return redirect:/user/list
     */
    @PostMapping("/edit/{id}")
    public String processRequestEditUser(@PathVariable Long id, @Valid User user,
                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/user/edit/" + id;
        } else {
            userService.editUser(user);
            return "redirect:/user/list";
        }
    }

    /**
     * /user/delete/{id}
     * <p>
     * Deletes user
     *
     * @param id variable type long user id
     * @return redirect:/user/list
     */
    @GetMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(userService.showUser(id));
        return "redirect:/user/list";
    }

}

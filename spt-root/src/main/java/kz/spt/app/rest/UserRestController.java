package kz.spt.app.rest;

import kz.spt.api.bootstrap.datatable.Page;
import kz.spt.api.bootstrap.datatable.PagingRequest;
import kz.spt.api.model.User;
import kz.spt.app.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest/users")
public class UserRestController {

    private UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;

    }

    @PostMapping
    public Page<User> list(@RequestBody PagingRequest pagingRequest) {
        return userService.getUsers(pagingRequest);
    }


}
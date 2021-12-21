package kz.spt.app.controller;

import kz.spt.lib.model.User;
import kz.spt.lib.model.dto.auth.ChangePasswordDto;
import kz.spt.lib.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.thymeleaf.util.StringUtils;

import javax.validation.Valid;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

@Controller
@RequiredArgsConstructor
@PreAuthorize("")
public class AuthController {
    private final UserService userService;

    @GetMapping("/change-password")
    public String showRegistrationPage(Model model, Authentication authentication){
        var userDetails = (UserDetails) authentication.getPrincipal();
        var user = ChangePasswordDto.builder()
                .userName(userDetails.getUsername())
                .build();
        model.addAttribute("user", ChangePasswordDto.builder().build());
        return "change-password";
    }

    @PostMapping("/change-password")
    public String processRegistrationForm(Model model, @Valid ChangePasswordDto user, BindingResult bindingResult, Authentication authentication) {
        var userDetails = (UserDetails) authentication.getPrincipal();
        Locale locale = LocaleContextHolder.getLocale();
        String language = "en";
        if (locale.toString().equals("ru")) {
            language = "ru-RU";
        }

        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.forLanguageTag(language));

        User userFromDB = userService.findByUsername(userDetails.getUsername());
        if (userFromDB == null) {
            ObjectError error = new ObjectError("usernameIsNull", bundle.getString("user.usernameIsNull"));
            bindingResult.addError(error);
        }
        if(StringUtils.isEmpty(user.getOldPassword())){
            ObjectError error = new ObjectError("passwordIsNull", bundle.getString("user.notCorrectPassword"));
            bindingResult.addError(error);
        }
        if(StringUtils.isEmpty(user.getNewPassword())){
            ObjectError error = new ObjectError("passwordIsNull", bundle.getString("user.notCorrectPassword"));
            bindingResult.addError(error);
        }

        if(!bindingResult.hasErrors()) {
            userFromDB.setPassword(user.getNewPassword());
            userService.saveUser(userFromDB);
            user.setOldPassword("");
            user.setNewPassword("");
        }
        model.addAttribute("user", user);
        return "change-password";
    }
}

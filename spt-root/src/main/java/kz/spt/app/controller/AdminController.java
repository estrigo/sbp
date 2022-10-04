package kz.spt.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.spt.lib.service.GitInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/admin-place")
@RequiredArgsConstructor
public class AdminController {


    private final GitInfoService gitInfoService;

    @GetMapping("/gitinfo")
    public String getOperations(Model model) {
        ObjectMapper oMapper = new ObjectMapper();
        Map<String, Object> map = oMapper.convertValue(gitInfoService.gitInfo(), Map.class);
        model.addAttribute("gitInfo", map);
        return "admin-place/git-info";
    }
}

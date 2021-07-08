package kz.spt.whitelistplugin.controller;

import kz.spt.whitelistplugin.model.Whitelist;
import kz.spt.whitelistplugin.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecordController {

    @Autowired
    RecordRepository recordRepository;
    @GetMapping("/record/")
    public List<Whitelist> getList() {
        return recordRepository.findAll();
    }

    @GetMapping("/record/add")
    public void addTest() {
        Whitelist whitelist = new Whitelist();
        whitelist.setNumber("611SRA05");
        recordRepository.save(whitelist);
    }

    @GetMapping("/record/test-view")
    public String testView() {
        return "contract/add";
    }

}

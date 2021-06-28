package kz.spt.whitelistplugin.controller;

import kz.spt.whitelistplugin.model.Record;
import kz.spt.whitelistplugin.repository.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecordController {

    @Autowired
    RecordRepository recordRepository;
    @GetMapping("/record/")
    public List<Record> getList() {
        return recordRepository.findAll();
    }

    @GetMapping("/record/add")
    public void addTest() {
        Record record = new Record();
        record.setNumber("611SRA05");
        recordRepository.save(record);
    }

    @GetMapping("/record/test-view")
    public String testView() {
        return "contract/add";
    }

}

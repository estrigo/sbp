package kz.spt.app.controller;

import kz.spt.lib.model.CarState;
import kz.spt.lib.model.dto.CarStateDto;
import kz.spt.lib.model.dto.CarStateFilterDto;
import kz.spt.lib.model.dto.EventFilterDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

@Controller
@RequestMapping("/journal")
public class CarStateController {

    private final String dateformat = "yyyy-MM-dd'T'HH:mm";

    @GetMapping("/list")
    public String showAllCarStates(Model model) {
        CarStateFilterDto carStateDtoFilter = null;
        if(!model.containsAttribute("carStateDtoFilter")){
            SimpleDateFormat format = new SimpleDateFormat(dateformat);
            carStateDtoFilter = new CarStateFilterDto();

            Calendar calendar = Calendar.getInstance();
            Date dateTo = calendar.getTime();
            carStateDtoFilter.dateToString = format.format(dateTo);

            calendar.add(Calendar.MONTH, -1);
            Date dateFrom = calendar.getTime();
            carStateDtoFilter.dateFromString = format.format(dateFrom);
            model.addAttribute("carStateDtoFilter", carStateDtoFilter);
        }
        return "journal/list";
    }

    @PostMapping("/list")
    public String processRequestSearch(Model model, @Valid @ModelAttribute("carStateDtoFilter") CarStateFilterDto carStateDtoFilter, BindingResult bindingResult) throws ParseException {
        return "journal/list";
    }
}

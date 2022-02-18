package kz.spt.abonomentplugin.dto;

import kz.spt.abonomentplugin.model.Abonoment;
import kz.spt.abonomentplugin.model.AbonomentTypes;
import kz.spt.lib.utils.StaticValues;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.beans.SimpleBeanInfo;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor
public class AbonomentDTO {

    public Long id;
    public String platenumber;
    public String begin;
    public String end;
    public int months;
    public Boolean paid;
    public BigDecimal price;
    public String parking;

    public static AbonomentDTO convertToDto(Abonoment abonoment){
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);

        AbonomentDTO dto = new AbonomentDTO();
        dto.id = abonoment.getId();
        dto.platenumber = abonoment.getCar().getPlatenumber();
        dto.begin = format.format(abonoment.getBegin());
        dto.end = format.format(abonoment.getEnd());
        dto.months = abonoment.getMonths();
        dto.parking = abonoment.getParking().getName();
        dto.price = abonoment.getPrice();
        dto.paid = abonoment.getPaid();
        return dto;
    }

    public static List<AbonomentDTO> convertToDto(List<Abonoment> abonoments){
        List<AbonomentDTO> dtoList = new ArrayList<>();
        for (Abonoment abonoment: abonoments){
            dtoList.add(convertToDto(abonoment));
        }
        return dtoList;
    }
}

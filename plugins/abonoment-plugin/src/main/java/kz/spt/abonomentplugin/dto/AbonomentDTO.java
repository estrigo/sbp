package kz.spt.abonomentplugin.dto;

import kz.spt.abonomentplugin.model.Abonement;
import kz.spt.lib.utils.StaticValues;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    public String type;
    public String created;

    public static AbonomentDTO convertToDto(Abonement abonement){
        SimpleDateFormat format = new SimpleDateFormat(StaticValues.dateFormat);

        AbonomentDTO dto = new AbonomentDTO();
        dto.id = abonement.getId();
        dto.platenumber = abonement.getCar().getPlatenumber();
        dto.begin = format.format(abonement.getBegin());
        dto.end = format.format(abonement.getEnd());
        dto.months = abonement.getMonths();
        dto.parking = abonement.getParking().getName();
        dto.price = abonement.getPrice();
        dto.paid = abonement.getPaid();
        dto.type = abonement.getType();
        if(abonement.getCreated() != null){
            dto.created = format.format(abonement.getCreated());
        }
        return dto;
    }

    public static List<AbonomentDTO> convertToDto(List<Abonement> abonements){
        List<AbonomentDTO> dtoList = new ArrayList<>(abonements.size());
        for (Abonement abonement : abonements){
            dtoList.add(convertToDto(abonement));
        }
        return dtoList;
    }
}

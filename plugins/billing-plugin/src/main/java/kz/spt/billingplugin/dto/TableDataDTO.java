package kz.spt.billingplugin.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Data
@NoArgsConstructor

public class TableDataDTO {

    public int draw;
    public int recordsTotal;
    public int recordsFiltered;
    public List<Object> data = new ArrayList<>();
}


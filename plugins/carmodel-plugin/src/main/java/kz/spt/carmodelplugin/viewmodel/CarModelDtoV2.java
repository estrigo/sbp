package kz.spt.carmodelplugin.viewmodel;

import lombok.Data;

import java.time.LocalTime;

@Data
public class CarModelDtoV2 {
    private Integer id;
    private String model;
    private Integer type; //types: 1 - passenger car	1, gazelle	2, truck 3
    private String updatedBy;
    private LocalTime updatedTime;
    private int page;
    private int elements;
}

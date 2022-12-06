package kz.spt.billingplugin.dto;

import lombok.Data;

@Data
public class HeaderDto {
    private String key;
    private String value;

    public HeaderDto() {
    }

    public HeaderDto(String key, String value) {
        this.key = key;
        this.value = value;
    }
}

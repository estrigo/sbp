package kz.spt.lib.model.dto;

import lombok.Builder;

@Builder
public class SelectOption {
    public String label;
    public String value;

    public SelectOption(String value,String label) {
        this.value = value;
        this.label = label;
    }
}

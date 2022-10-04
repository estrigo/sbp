package kz.spt.app.model.dto;

import lombok.Data;

@Data
@SuppressWarnings("PMD.UnusedPrivateField")
public class GenericResponse<T> {

    private String UID;
    private T resultData;

    public GenericResponse() {
    }

    public GenericResponse(String UID, T resultData) {
        this.UID = UID;
        this.resultData = resultData;
    }
}

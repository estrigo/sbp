package kz.spt.lib.model.dto.adminPlace.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.Serializable;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonDeserialize(using = AdminRequestResponseTypeDeserializer.class)
public enum AdminRequestResponseType implements Serializable {
    GIT_INFO,
    UPDATE_PROPERTY,
    START_WHITE_LIST_JOB,
    STOP_WHITE_LIST_JOB,
    UPLOAD_WHITELIST,
    DELETED_WHITELIST;

    public String getType() {
        return this.name();
    }


    public static AdminRequestResponseType search (String name){
        for (AdminRequestResponseType type : AdminRequestResponseType.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

}

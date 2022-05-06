package kz.spt.megaplugin.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestThPP {
    private String command;
    private String platenumber;
    private String parking_uid;
    private String type;

}

package kz.spt.lib.model.dto.carmen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarmenAuthResponse {
    private String Type;
    private Data Data;

    public class Data{
        private String sid;

        public String getSid() {
            return sid;
        }
    }
}

package kz.spt.lib.model.dto.carmen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.json.JSONObject;

import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarmenEvent {
    public Map<String, String> header;
    public CarmenImage associatedFrame;
    public JSONObject data;
}

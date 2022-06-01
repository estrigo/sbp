package kz.spt.lib.model.dto.carmen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarmenImage {
    public Map<String, String> header;
    public byte[] content;
}

package kz.spt.lib.model.dto.carmen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.Future;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarmenThread {
    private boolean active=false;
    private Future task;
}

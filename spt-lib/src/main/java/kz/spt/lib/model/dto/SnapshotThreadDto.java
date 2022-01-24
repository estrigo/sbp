package kz.spt.lib.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SnapshotThreadDto {
    private boolean isActive;
    private Thread thread;
}

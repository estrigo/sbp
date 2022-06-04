package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "camera_tab")
@Audited
public class CameraTab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "cameraTab")
    private List<Camera> cameraList;

    @Column(name = "snapshot_enabled")
    private Boolean snapshotEnabled = true;

    @Column(name = "link_order")
    private Integer linkOrder;
}

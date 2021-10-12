package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "camera")
@Audited
public class Camera {

    public enum CameraType {
        FRONT,
        BACK;

        public static final Camera.CameraType[] ALL = {FRONT,BACK};
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @Column(unique=true)
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_type")
    private CameraType cameraType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate")
    private Gate gate;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;
}
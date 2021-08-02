package kz.spt.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "camera")
public class Camera {

    public enum CameraType {
        FRONT,
        BACK
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
package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalTime;
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

    private Integer timeout;

    private String ip;

    private String login;

    private String password;

    private String detectorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "camera_type")
    private CameraType cameraType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate")
    private Gate gate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "camera_tab")
    private CameraTab cameraTab;

    @CreationTimestamp
    private Date created;

    @UpdateTimestamp
    private Date updated;

    private String snapshotUrl;

    @Column(columnDefinition="tinyint(1) default 1")
    private boolean enabled = true;

    private String carmenIp;
    private String carmenLogin;
    private String carmenPassword;
    private Boolean snapshotEnabled;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;
    private Date updatedTime;
    private String updatedTimeBy;

}
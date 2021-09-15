package kz.spt.lib.model;

import kz.spt.lib.extension.HashMapConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event_log")
public class EventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long objectId;

    private String objectClass;

    private String description;

    private String plateNumber;

    @Column(name = "properties", columnDefinition = "text")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Object> properties;

    @CreationTimestamp
    private Date created;

    public String getNullSafePlateNumber() {
        return getPlateNumber() != null ? this.plateNumber : "";
    }

    public String getNullSafeDescription() {
        return getDescription() != null ? this.description : "";
    }
}

package kz.spt.lib.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import kz.spt.lib.extension.HashMapConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

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

    private String descriptionEn;

    private String plateNumber;

    @Column(name = "properties", columnDefinition = "text")
    @Convert(converter = HashMapConverter.class)
    private Map<String, Object> properties;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = JsonFormat.DEFAULT_TIMEZONE)
    @CreationTimestamp
    private Date created;

    public String getNullSafePlateNumber() {
        return getPlateNumber() != null ? this.plateNumber : "";
    }

    public String getNullSafeDescription() {
        return getDescription() != null ? this.description : "";
    }

    public String getNullSafeDescriptionEn() {
        return getDescriptionEn() != null ? this.descriptionEn : "";
    }

}

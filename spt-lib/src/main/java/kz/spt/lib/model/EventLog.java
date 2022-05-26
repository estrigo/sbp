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

    public enum EventType {
        MANUAL_GATE_OPEN,
        MANUAL_GATE_CLOSE,
        DEBT,NOT_ENOUGH_BALANCE,
        PASS,NOT_PASS,
        ERROR,NEW_CAR_DETECTED,
        ZERO_TOUCH,FIFTEEN_FREE,PREPAID,WHITELIST,
        PAID, ABONEMENT
    }

    public enum StatusType{
        Allow,
        Deny,
        Error,
        Success,
        Debt,
        NotFound,
        Warning
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long objectId;

    private String objectClass;

    private String description;

    private String descriptionEn;

    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventLog.EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_type")
    private EventLog.StatusType statusType;

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

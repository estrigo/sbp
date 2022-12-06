package kz.spt.lib.model;

import kz.spt.lib.enums.SyslogTypeEnum;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@Table(name = "syslog")
public class Syslog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description_ru")
    private String descriptionRu;

    @Column(name = "description_en")
    private String descriptionEn;

    private Date created;

    @Enumerated(EnumType.STRING)
    @Column(name = "syslog_type")
    private SyslogTypeEnum syslogType;

    private String UUID;

    @Column(name = "message", columnDefinition = "text")
    private String message;

}

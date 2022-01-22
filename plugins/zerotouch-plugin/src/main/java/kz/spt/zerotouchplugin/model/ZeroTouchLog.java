package kz.spt.zerotouchplugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Data
@Audited
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "zero_touch_log")
public class ZeroTouchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "car_state_id")
    private Long carStateId;

    private String platenumber;

    private BigDecimal rate;

    private String provider;

    private String request;

    private String resonse;

    private Boolean isPaid;

    private Long txtID;
}

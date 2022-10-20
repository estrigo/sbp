package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "model", nullable = false, unique = true)
    private String model;
    @Column(name = "type")
    private Integer type; //types: 1 - passenger car	1, gazelle	2, truck 3
    @DateTimeFormat
    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "updated_Time")
    private LocalDateTime updatedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    private Dimensions dimensions;
}

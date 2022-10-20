package kz.spt.lib.model;

import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dimensions")
@NoArgsConstructor
public class Dimensions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "car_classification", unique = true)
    private String carClassification;

    @Column(name = "updated_by")
    private String updatedBy;
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;



    public Dimensions(Long id, String carClassification, String updatedBy, LocalDateTime updatedTime) {
        this.id = id;
        this.carClassification = carClassification;
        this.updatedBy = updatedBy;
        this.updatedTime = updatedTime;
    }


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getCarClassification() {
        return carClassification;
    }
    public void setCarClassification(String carClassification) {
        this.carClassification = carClassification;
    }
    public String getUpdatedBy() {
        return updatedBy;
    }
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }
    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }
}

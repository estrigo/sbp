package kz.smartparking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cars", schema = "crm")
public class Cars {

    @Id
    private String numberplate;

    private String color;

    private String brand;

    private Boolean deleted = false;
}

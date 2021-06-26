package kz.smartparking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "white_list", schema = "crm")
public class WhiteList {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    String plate_number;

    @ManyToOne
    private Customer customer;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate beginDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}

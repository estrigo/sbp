package kz.spt.lib.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "reasons")
public class Reasons {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String reasonRu;

    private String reasonEn;
}

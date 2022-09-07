package kz.spt.prkstatusplugin.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "pos_terminal")
public class PosTerminal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String ip;

    private Boolean reconsilated;
}

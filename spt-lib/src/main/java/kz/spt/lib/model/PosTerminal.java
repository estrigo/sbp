package kz.spt.lib.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "pos_terminal")
public class PosTerminal {

    public enum terminalType {
        TERMINAL,
        PARKOMAT;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    private String ip;

    private String apikey;

    private Boolean reconsilated;

    @Enumerated(EnumType.STRING)
    private terminalType type;
}

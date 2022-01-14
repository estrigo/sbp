package kz.spt.lib.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "blacklist")
public class Blacklist {
    public enum BlacklistType{
        OTHER,
        MORE_16H
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true)
    private String plateNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private Blacklist.BlacklistType type;
}

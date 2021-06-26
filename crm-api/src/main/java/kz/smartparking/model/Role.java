package kz.smartparking.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "role", schema = "crm")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "role_id")
    private int id;

    @Column(name = "role", unique = true)
    private String name;

    @Column(name = "plugin")
    private String plugin;
}

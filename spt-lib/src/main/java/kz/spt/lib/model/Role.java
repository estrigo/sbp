package kz.spt.lib.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Data
@Table(name = "role")
public class Role implements Comparable<Role>{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private int id;

    @Column(name = "role", unique = true)
    private String name;

    @Column(name = "name_ru")
    private String name_ru;

    @Column(name = "name_en")
    private String name_en;

    @Column(name = "plugin")
    private String plugin;

    @Override
    public int compareTo(Role o) {
        return this.name.compareTo(o.name);
    }
}

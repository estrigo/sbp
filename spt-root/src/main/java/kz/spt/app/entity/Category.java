package kz.spt.app.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "category", schema = "crm")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "category")
    private String name;

}

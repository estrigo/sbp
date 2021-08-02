package kz.spt.api.model;

import kz.spt.api.revision.SptRevisionListener;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionEntity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RevisionEntity(SptRevisionListener.class)
@Table(name = "spt_revision")
public class SptRevEntity extends DefaultRevisionEntity {

    private String username;

}

package kz.spt.api.model;

import kz.spt.api.model.EventLog;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(EventLog.class)
public class EventLog_ {

    public static volatile SingularAttribute<EventLog, Long> id;

    public static volatile SingularAttribute<EventLog, Date> created;

    public static volatile SingularAttribute<EventLog, String> plateNumber;

    public static volatile SingularAttribute<EventLog, String> description;

}

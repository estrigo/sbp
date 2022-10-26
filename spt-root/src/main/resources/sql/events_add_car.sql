alter table event_log add column car bigint;
alter table add constraint FKrubekc7uoggvqpfpl5rt416wj foreign key (car) references cars (id);

UPDATE event_log a
    INNER JOIN cars c on a.plate_number = c.platenumber
    SET a.car = c.id;
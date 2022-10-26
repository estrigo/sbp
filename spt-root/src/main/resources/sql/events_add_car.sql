UPDATE event_log a
    INNER JOIN cars c on a.plate_number = c.platenumber
    SET a.car = c.id;
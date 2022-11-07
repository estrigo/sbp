UPDATE transaction a
    INNER JOIN cars c on a.plate_number = c.platenumber
    SET a.car = c.id
where a.car is null;
UPDATE payments a
    INNER JOIN cars c on a.car_number = c.platenumber
    SET a.car = c.id
where a.car is null;
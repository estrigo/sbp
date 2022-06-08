CREATE PROCEDURE fixDublicatePayments()
BEGIN
    DECLARE bDone INT;
    DECLARE vid bigint;
    DECLARE vplateNumber VARCHAR(255);
    DECLARE vcar_state_id bigint;
    DECLARE vamount decimal(8,2);

    DECLARE curs CURSOR FOR  select max(tr.id) as id, tr.plate_number, tr.car_state_id, tr.amount
                             from transaction tr
                                      inner join transaction old on tr.plate_number = old.plate_number and date_format(tr.date, '%d.%m.%y %H:%i') = date_format(old.date, '%d.%m.%y %H:%i') and
                                                                    tr.car_state_id = old.car_state_id and
                                                                    tr.amount = old.amount
                                      inner join balance b on tr.plate_number = b.plate_number and b.balance < 0
                             where tr.id <> old.id
                             group by tr.plate_number, tr.car_state_id, tr.amount
                             order by id;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET bDone = 1;
OPEN curs;

SET bDone = 0;
    REPEAT
FETCH curs INTO vid, vplateNumber, vcar_state_id, vamount;
delete from transaction where id = vid;
update balance set balance = balance + abs(vamount) where plate_number = vplateNumber;
UNTIL bDone END REPEAT;

CLOSE curs;

update balance set balance = balance + vamount where plate_number = vplateNumber;
END;

CALL fixDublicatePayments();

DROP PROCEDURE fixDublicatePayments;
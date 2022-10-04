create table if not exists billing_payments_rep
(
    id bigint auto_increment
        primary key,
    onDay datetime null,
    totalSum bigint null,
    prov varchar(30) null
);

create table if not exists billing_rep
(
    id bigint auto_increment
        primary key,
    onDay datetime null,
    records bigint null,
    paymentRecords bigint null,
    whitelistRecords bigint null,
    abonementRecords bigint null,
    freeMinuteRecords bigint null,
    debtRecords bigint null,
    fromBalanceRecords bigint null,
    freeRecords bigint null,
    autoClosedRecords bigint null,
    thirdPartyRecords bigint null
);

create definer = root@localhost procedure getBillingPayments()
BEGIN
SET @sql = NULL;
SELECT GROUP_CONCAT(
               CONCAT('sum(case when prov = ''', prov, ''' then totalSum else 0 end) as ', prov)
           )
INTO @sql
FROM
    (
    SELECT DISTINCT prov from billing_payments_rep
    #     where name not like ('%test%')

    ) t;

SET @sql = CONCAT('select onDay, ', @sql, ' from billing_payments_rep pt
group by onDay');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
END;

create definer = root@localhost procedure run_billingrep()
BEGIN

    -- declare the program variables where we'll hold the values we're sending into the procedure;
-- declare as many of them as there are input arguments to the second procedure,
-- with appropriate data types.

DECLARE dateFrom datetime DEFAULT NULL;
DECLARE dateTo datetime DEFAULT NULL;

-- we need a boolean variable to tell us when the cursor is out of data

DECLARE done TINYINT DEFAULT FALSE;

declare records bigint default 0;
declare paymentRecords bigint default 0;
declare whitelistRecords bigint default 0;
declare abonementRecords bigint default 0;
declare freeMinuteRecords bigint default 0;
declare debtRecords bigint default 0;
declare fromBalanceRecords bigint default 0;
declare freeRecords bigint default 0;
declare autoClosedRecords bigint default 0;
declare thirdPartyRecords bigint default 0;

-- declare a cursor to select the desired columns from the desired source table1
-- the input argument (which you might or might not need) is used in this example for row selection

DECLARE cursor1 -- cursor1 is an arbitrary label, an identifier for the cursor
    CURSOR FOR
    select distinct(date(cs.out_timestamp)) as dateFrom, adddate(date(cs.out_timestamp), interval 1 day)
    from car_state cs
             left join billing_rep b on date(b.onDay) = date(cs.out_timestamp)
    where b.onDay is null
      and cs.out_timestamp is not null
      and date(cs.out_timestamp) < current_date();

-- this fancy spacing is of course not required; all of this could go on the same line.

-- a cursor that runs out of data throws an exception; we need to catch this.
-- when the NOT FOUND condition fires, "done" -- which defaults to FALSE -- will be set to true,
-- and since this is a CONTINUE handler, execution continues with the next statement.

DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

-- open the cursor

OPEN cursor1;

my_loop: -- loops have to have an arbitrary label; it's used to leave the loop
    LOOP

        -- read the values from the next row that is available in the cursor

FETCH cursor1 INTO dateFrom, dateTo;

IF done THEN -- this will be true when we are out of rows to read, so we go to the statement after END LOOP.
            LEAVE my_loop;
ELSE -- val1 and val2 will be the next values from c1 and c2 in table t1,
        -- so now we call the procedure with them for this "row"
set records = (select count(cs.id) as count
               from car_state cs
               where cs.out_timestamp between dateFrom and dateTo);
set whitelistRecords = (select count(distinct cs.car_state_id)
                        from (select l.created, l.plate_number
                              from event_log l
                              where l.object_class = 'Gate'
                                and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                                and l.event_type = 'WHITELIST_OUT') l
                                 inner join (select cs.id as car_state_id, cs.out_timestamp, cs.car_number
                                             from car_state cs
                                             where cs.out_timestamp between dateFrom and dateTo
                                               and cs.out_gate is not null) cs
                                            on cs.car_number = l.plate_number and
                                               cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)
);
set paymentRecords = (select count(distinct cs.car_state_id)
                      from (
                               select l.created, l.plate_number
                               from event_log l
                               where l.object_class = 'Gate'
                                 and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                                 and l.event_type = 'PAID_PASS'
                           ) l
                               inner join (
                          select p.car_state_id as car_state_id,
                                 cs.out_timestamp,
                                 cs.car_number,
                                 sum(p.amount)  as totalSumma
                          from payments p
                                   inner join car_state cs on cs.id = p.car_state_id
                          where (p.out_date between dateFrom and dateTo or p.out_date is null)
                            and cs.out_timestamp between dateFrom and dateTo
                            and cs.out_gate is not null
                          group by p.car_state_id
                          having totalSumma > 0
                      ) cs on cs.car_number = l.plate_number and
                              cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second));
set freeRecords = (select count(distinct cs.car_state_id)
                   from (select l.created, l.plate_number
                         from event_log l
                         where l.object_class = 'Gate'
                           and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                           and l.event_type = 'FREE_PASS') l
                            inner join (select cs.id as car_state_id, cs.out_timestamp, cs.car_number
                                        from car_state cs
                                        where cs.out_timestamp between dateFrom and dateTo
                                          and cs.out_gate is not null) cs
                                       on cs.car_number = l.plate_number and
                                          cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second)
);
set autoClosedRecords =
        (select count(cs.id)
         from car_state cs
         where cs.out_timestamp between dateFrom and dateTo
           and cs.out_gate is null);
set fromBalanceRecords = (select count(distinct cs.car_state_id)
                          from (
                                   select l.created, l.plate_number
                                   from event_log l
                                   where l.object_class = 'Gate'
                                     and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                                     and l.event_type = 'PAID_PASS'
                               ) l
                                   inner join (
                              select cs.id as car_state_id, cs.out_timestamp, cs.car_number
                              from car_state cs
                                       left outer join (
                                  select p.car_state_id
                                  from payments p
                                  where p.out_date between dateFrom and dateTo
                                     or p.out_date is null
                                  group by p.car_state_id
                                  having sum(p.amount) > 0
                              ) payments on payments.car_state_id = cs.id
                              where cs.out_timestamp between dateFrom and dateTo
                                and cs.out_gate is not null
                                and payments.car_state_id is null
                          ) cs on cs.car_number = l.plate_number and
                                  cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second));

set debtRecords = (select count(distinct cs.car_state_id)
                   from (
                            select l.created, l.plate_number
                            from event_log l
                            where l.object_class = 'Gate'
                              and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                              and l.event_type = 'DEBT_OUT'
                        ) l
                            inner join (
                       select cs.id as car_state_id, cs.out_timestamp, cs.car_number
                       from car_state cs
                       where cs.out_timestamp between dateFrom and dateTo
                         and cs.out_gate is not null
                   ) cs on cs.car_number = l.plate_number and
                           cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second));

set freeMinuteRecords = (select count(distinct cs.car_state_id)
                         from (
                                  select l.created, l.plate_number
                                  from event_log l
                                  where l.object_class = 'Gate'
                                    and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                                    and l.event_type = 'FIFTEEN_FREE'
                              ) l
                                  inner join (
                             select cs.id as car_state_id, cs.out_timestamp, cs.car_number
                             from car_state cs
                             where cs.out_timestamp between dateFrom and dateTo
                               and cs.out_gate is not null
                         ) cs on cs.car_number = l.plate_number and
                                 cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second));

set abonementRecords = (select count(distinct cs.car_state_id)
                        from (
                                 select l.created, l.plate_number
                                 from event_log l
                                 where l.object_class = 'Gate'
                                   and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                                   and l.event_type = 'ABONEMENT_PASS'
                             ) l
                                 inner join (
                            select cs.id as car_state_id, cs.out_timestamp, cs.car_number
                            from car_state cs
                            where cs.out_timestamp between dateFrom and dateTo
                              and cs.out_gate is not null
                        ) cs on cs.car_number = l.plate_number and
                                cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second));

set thirdPartyRecords = (select count(distinct cs.car_state_id)
                         from (
                                  select l.created, l.plate_number
                                  from event_log l
                                  where l.object_class = 'CarState'
                                    and l.created between date_sub(dateFrom, interval 1 minute) and adddate(dateTo, interval 1 minute)
                                    and l.event_type = 'PREPAID'
                              ) l
                                  inner join (
                             select cs.id as car_state_id, cs.out_timestamp, cs.car_number
                             from car_state cs
                             where cs.out_timestamp between dateFrom and dateTo
                               and cs.out_gate is not null
                         ) cs on cs.car_number = l.plate_number and
                                 cs.out_timestamp between date_sub(l.created, INTERVAL 60 second) and date_add(l.created, INTERVAL 60 second));
insert into billing_rep(onDay, records, whitelistRecords, freeRecords, thirdPartyRecords, abonementRecords,
                        freeMinuteRecords, debtRecords, fromBalanceRecords, autoClosedRecords,
                        paymentRecords)
    value (dateFrom, records, whitelistRecords, freeRecords, thirdPartyRecords, abonementRecords,
              freeMinuteRecords, debtRecords, fromBalanceRecords, autoClosedRecords, paymentRecords);
-- maybe do more stuff here
insert into billing_payments_rep(onDay, totalSum, prov)
select DATE(p.out_date) as date_out,
       sum(p.amount)    as totalSumma,
       pp.provider      as "prov"
from car_state cs
         left outer join payments p on cs.id = p.car_state_id
         inner join payment_provider pp on p.provider_id = pp.id

where (p.out_date between dateFrom and dateTo
    or p.out_date is null)
  and cs.out_timestamp between dateFrom and dateTo
group by DATE(p.out_date), prov
union all
select DATE(p.out_date) as date_out,
       sum(p.amount)    as totalSumma,
       'totalSum'     as "prov"
from car_state cs
         left outer join payments p on cs.id = p.car_state_id
         inner join payment_provider pp on p.provider_id = pp.id

where (p.out_date between dateFrom and dateTo
    or p.out_date is null)
  and cs.out_timestamp between dateFrom and dateTo
group by DATE(p.out_date)
union all
select DATE(p.out_date) as date_out,
       sum(p.amount)    as totalSumma,
       'cardsSumma'     as "prov"
from car_state cs
         left outer join payments p on cs.id = p.car_state_id and p.ikkm = true
         inner join payment_provider pp on p.provider_id = pp.id and pp.cashless_payment = false

where (p.out_date between dateFrom and dateTo
    or p.out_date is null)
  and cs.out_timestamp between dateFrom and dateTo
group by DATE(p.out_date), prov
union all
select DATE(p.out_date) as date_out,
       sum(p.amount)    as totalSumma,
       'cashSumma'      as "prov"
from car_state cs
         left outer join payments p on cs.id = p.car_state_id and (p.ikkm is null or p.ikkm = false)
         inner join payment_provider pp on p.provider_id = pp.id and pp.cashless_payment = false

where (p.out_date between dateFrom and dateTo
    or p.out_date is null)
  and cs.out_timestamp between dateFrom and dateTo
group by DATE(p.out_date), prov
order by 1;

END IF;
END LOOP;


-- execution continues here when LEAVE my_loop is encountered;
-- you might have more things you want to do here

-- the cursor is implicitly closed when it goes out of scope, or can be explicitly closed if desired

CLOSE cursor1;

END;

create or replace view billing_rep_pivot as
select onDay, 'records', records as count from billing_rep union all
select onDay, 'paymentRecords', paymentRecords as count from billing_rep union all
select onDay, 'whitelistRecords', whitelistRecords from billing_rep union all
select onDay, 'abonementRecords', abonementRecords from billing_rep union all
select onDay, 'freeMinuteRecords', freeMinuteRecords from billing_rep union all
select onDay, 'debtRecords', debtRecords from billing_rep union all
select onDay, 'fromBalanceRecords', fromBalanceRecords from billing_rep union all
select onDay, 'freeRecords', freeRecords from billing_rep union all
select onDay, 'autoClosedRecords', autoClosedRecords from billing_rep union all
select onDay, 'thirdPartyRecords', thirdPartyRecords from billing_rep order by onDay;

call run_billingrep();


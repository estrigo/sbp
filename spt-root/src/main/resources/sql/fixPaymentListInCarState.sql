update car_state cs
    inner join car_state_aud csa on cs.id = csa.id and csa.out_timestamp is null and csa.out_gate is null and csa.payment_json is not null and csa.amount is not null
    inner join car_state_aud csa2 on csa.id = csa2.id and csa.rev < csa2.rev and csa2.out_timestamp is not null and csa2.out_gate is not null and csa2.payment_json is null and csa2.amount is null
    set cs.payment_json = csa.payment_json, cs.amount = csa.amount, cs.payment_id = csa.payment_id
where cs.rate_amount  is not null
  and cs.out_timestamp is not null
  and cs.out_gate is not null
  and cs.payment_json is null
  and cs.amount is null;
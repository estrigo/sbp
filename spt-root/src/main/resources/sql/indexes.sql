CREATE INDEX out_timestamp_idx ON car_state (out_timestamp);
create index car_number_idx on car_state (car_number);
create index event_type_idx on event_log (event_type);
CREATE INDEX created_idx ON event_log (created);


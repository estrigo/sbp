insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (1, 'ROLE_ADMIN', null ,'Administrator','Администратор', 'Administrator');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (2, 'ROLE_USER', null,'User','Пользователь', 'Benutzer');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (3, 'ROLE_MANAGER', null,'Manager','Менеджер', 'Manager');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (4, 'ROLE_OWNER', null,'Owner','Владелец', 'Eigentümer');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (5, 'PLUGIN_ROLE_TEST', 'test-plugin','Test user','Тестовая роль', 'Test-Benutzer');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (6, 'ROLE_OPERATOR', null, 'Operator', 'Оператор', 'Operator');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (7, 'ROLE_OPERATOR_NO_REVENUE_SHARE', null,'Operator NO REVENUE SHARE','Оператор для контрактов без долевого участия', 'Operator KEINE EINNAHMENBETEILIGUNG');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (9, 'ROLE_READ', null,'read','read', 'lesen');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (30, 'ROLE_BAQORDA', null,'read','read', 'lesen');
insert into role (role_id, role, plugin, name_en, name_ru, name_de) values (31, 'ROLE_ACCOUNTANT', null,'Accountant','Бугхалтер', 'Buchhalter');
insert into role (role, plugin, name_en, name_ru, name_de) values ('ROLE_OPERATOR_PARQOUR', null, 'Operator Parqour', 'Оператор Parqour', 'Operator Parqour');


INSERT INTO users (id, email, enabled, first_name, last_name, password, username)
VALUES ('1', 'a@u', '1', 'AFN', 'ALN', '$2a$10$vukbRa3lWnWvaOF4gkwdfexE.EfUpwQQJVZzisriuh5rOl4HfMXuy', 'admin');
# admin - pass = admin
INSERT INTO user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO user_role (user_id, role_id) VALUES (1, 2);
insert into role (role_id, role, plugin, name_en, name_ru) values (1, 'ROLE_ADMIN', null ,'Administrator','Администратор'),
                                                (2, 'ROLE_USER', null,'User','Пользователь'),
                                                (3, 'ROLE_MANAGER', null,'Manager','Менеджер'),
                                                (4, 'ROLE_OWNER', null,'Owner','Владелец'),
                                                (5, 'PLUGIN_ROLE_TEST', 'test-plugin','Test user','Тестовая роль');

INSERT INTO users (id, email, enabled, first_name, last_name, password, username)
VALUES ('1', 'a@u', '1', 'AFN', 'ALN', '$2a$10$iPgnenFIoM67cYL9let/iOLBphbDaEkAz3BmiXOCmWq5A4M2TkXAG', 'admin');
# admin - pass = admin
INSERT INTO user_role (user_id, role_id) VALUES (1, 1);
INSERT INTO user_role (user_id, role_id) VALUES (1, 2);
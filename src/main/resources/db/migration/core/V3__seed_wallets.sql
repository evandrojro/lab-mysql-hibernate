USE txlab_core;

INSERT INTO wallet (id, owner, balance)
VALUES (2, 'demo-b', 60.00)
ON DUPLICATE KEY UPDATE owner = VALUES(owner);

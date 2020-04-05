#Start service
#sudo /sbin/service mysqld start
#sudo chkconfig --level 3 mysqld on
#EC2 Secure MySQL server installation: 
#sudo /usr/bin/mysql_secure_installation
#login
#mysql -u root -p

#Create DATABASE
CREATE DATABASE LBM;

#Goto Database
Use LBM;

#Create Table user
CREATE TABLE L_USER (
    USERNAME varchar(7) PRIMARY KEY,
    PASSWORD varchar(7) NOT NULL
);

#Create Table for token
CREATE TABLE L_TOKEN (
    TOKEN varchar(12) PRIMARY KEY
);

#Create Table for Transaction
CREATE TABLE L_TRANSACTION (
    TRANSACTION_ID INT PRIMARY KEY,
    TOKEN varchar(12) NOT NULL,
    CREATE_TIME DATETIME NOT NULL
) ;

#Create Table for Book
CREATE TABLE L_BOOK (
    ID INT PRIMARY KEY AUTO_INCREMENT,
    TITLE varchar(255),
    AUTHOR varchar(255),
    PUBLISHER varchar(255),
    YEAR varchar(4),
    AVAILABLE BOOLEAN NOT NULL DEFAULT TRUE
);

#Create an user from any host, not recommended, replace % with our server when it is provided

CREATE USER 'sqlUser'@'%' IDENTIFIED BY 'sqlUserPwd10000';

GRANT SELECT ON LBM.L_USER TO 'sqlUser'@'%';

GRANT SELECT, INSERT, DELETE, UPDATE ON LBM.L_TOKEN TO 'sqlUser'@'%';

GRANT SELECT, INSERT, DELETE, UPDATE ON LBM.L_TRANSACTION TO 'sqlUser'@'%';

GRANT SELECT, INSERT, DELETE ON LBM.L_BOOK TO 'sqlUser'@'%';

flush privileges;

ALTER TABLE L_TRANSACTION ENGINE=MyISAM;

ALTER TABLE L_TOKEN ENGINE=MyISAM;

SET GLOBAL event_scheduler = ON;

SET GLOBAL wait_timeout=120;

#Create 100 users
DELIMITER $$
CREATE PROCEDURE populate (IN num int)
BEGIN
DECLARE i int DEFAULT 1;
WHILE i <= num do
INSERT INTO L_USER (USERNAME,PASSWORD) VALUES (CONCAT('user',LPAD(i, 3, 0)),CONCAT('pass',LPAD(i, 3, 0)));
SET i = i + 1;
END WHILE;
END
$$
DELIMITER ;

CALL populate(100);

DROP PROCEDURE populate;

#Delect Transaction > 2 mins, every sec checking

DELIMITER $$
CREATE EVENT delete_transaction
ON SCHEDULE EVERY 1 SECOND
DO BEGIN
      DELETE FROM L_TRANSACTION WHERE CREATE_TIME <= TIMESTAMPADD(MINUTE,-2,NOW());
END;
$$
DELIMITER ;
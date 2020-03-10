#Start service
#sudo /sbin/service mysqld start
#sudo chkconfig --level 3 mysqld on
#EC2 Secure MySQL server installation: 
#sudo /usr/bin/mysql_secure_installation
#login
#mysql -u root -p

#Create DATABASE
DROP DATABASE LBM;
CREATE DATABASE LBM;

#Goto Database
Use LBM;


#Create Tabor user
CREATE TABLE L_USER (
    USERNAME varchar(9) PRIMARY KEY,
    PASSWORD varchar(11) NOT NULL
);
#Create Table for token
CREATE TABLE L_TOKEN (
    TOKEN varchar(12) PRIMARY KEY
);
#Create Table for Transaction
CREATE TABLE L_TRANSACTION (
    TRANSACTION_ID varchar(12) PRIMARY KEY,
    BOOK_ID INT,
    ACTION varchar(6),
    CREATE_TIME DATETIME NOT NULL
) ;
#Create Table for Book
CREATE TABLE L_BOOK (
    ID INT PRIMARY KEY AUTO_INCREMENT,
    TITLE varchar(255),
    AUTHOR varchar(255),
    PUBLISHER varchar(255),
    YEAR varchar(4),
    AVAILABLE BOOLEAN
);




#Create 100 users
DELIMITER $$
CREATE PROCEDURE populate (IN num int)
BEGIN
DECLARE i int DEFAULT 1;
WHILE i <= num do
INSERT INTO L_USER (USERNAME,PASSWORD) VALUES (CONCAT('user',LPAD(i, 3, 0)),CONCAT('passwd',LPAD(i, 3, 0)));
SET i = i + 1;
END WHILE;
END
$$
DELIMITER ;
CALL populate(100);
DROP PROCEDURE populate;



#Delect Transaction > 2 mins, every sec checking
SET GLOBAL event_scheduler = ON;
DELIMITER $$
CREATE EVENT delete_transaction
ON SCHEDULE EVERY 1 SECOND
DO BEGIN
      DELETE FROM L_TRANSACTION WHERE CREATE_TIME <= TIMESTAMPADD(MINUTE,-15,NOW());
END;
$$
DELIMITER ;

#PROCEDURE for commit transaction

/*
DELIMITER $$
CREATE FUNCTION CommitTranaction ()
RETURNS BOOLEAN
BEGIN

END
$$
DELIMITER ;

#PROCEDURE for commit transaction
DELIMITER $$
CREATE FUNCTION CancelTranaction ()
RETURNS BOOLEAN
BEGIN

END
$$
DELIMITER ;
*/


flush privileges;
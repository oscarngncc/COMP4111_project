# COMP4111_project
Team:\
members:\
Oscar, Ng Chi Chung - 20422802\
Howard, Ling Hou Lam - 20517592


## RESTful Web Service for Library Book Management

Server Hosting Instruction:

Part 1: SQL Server:\
i. Install MySQL Server 5.7\
ii. Login with root account\
iii. Run DB_Initialize.sql

Part 2: Web Server\
Method1:
i. Install JDK 11\
ii. Download COMP4111_project.jar\
iii. Run COMP4111_project.jar like the image below\
![Image of Demo1](https://github.com/oscarngncc/COMP4111_project/blob/master/Annotation%202020-04-05%20154744.png)

Method2:
i. Install JDK 11\
ii. Compile every .java files in src folder with every .jar files in lib folder\
iii. Run the main class HttpServerHost with every .class files generated in step ii and every .jar files in lib folder\
Optional. HttpServerHost will connect to the DB URL "jdbc:mysql://localhost:3306/LBM" by default, if you connect to a remote SQL server, state the DB URL of the server as a string args when running HttpServerHost like the image below.\
![Image of Demo2](https://github.com/oscarngncc/COMP4111_project/blob/master/Annotation%202020-04-05%20144508.png)



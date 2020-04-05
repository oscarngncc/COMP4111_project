import json.Book;
import json.BookList;
import json.Transaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlHelpers {

    public static boolean IsUserFound(String username, String password){
        try{
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery("SELECT * FROM L_USER WHERE USERNAME = '" + username + "' AND PASSWORD = '" + password + "';");
            if (!results.next()){
                results.close();
                return false;
            }else{
                results.close();
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return true;
    }

    public static boolean InsertToken (String token){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("ROLLBACK;");
            command.execute("INSERT INTO L_TOKEN VALUES ('" + token + "', connection_id());");
            command.execute("commit;");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean DeleteToken (String token){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("ROLLBACK;");
            command.execute("DELETE FROM L_TOKEN WHERE TOKEN = '" + token + "' AND CONNECTION_ID = connection_id();");
            command.execute("commit;");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean IsTokenFound (String token){
        try{
            if(token != null) {
                Connection connection = SqlSingleton.getConnection();
                Statement command = connection.createStatement();
                ResultSet results = command.executeQuery(
                        "SELECT * FROM L_TOKEN WHERE TOKEN = '" + token + "' AND CONNECTION_ID = connection_id();"
                );
                if (!results.next()) {
                    results.close();
                } else {
                    results.close();
                    return true;
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public static boolean IsUserTokenFound (String userId){
        try{
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery("SELECT * FROM L_TOKEN WHERE TOKEN LIKE '" + userId + "%';");
            if (!results.next()) {
                results.close();
                return false;
            } else {
                results.close();
            }
        }catch(SQLException e){
            e.printStackTrace();
        }
        return true;
    }

    public static int FindIdenticalBook (Book book) {
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery(
                    "SELECT * FROM L_BOOK WHERE TITLE = '" +
                            book.getTitle() + "' AND AUTHOR = '" +
                            book.getAuthor() + "' AND PUBLISHER = '" +
                            book.getPublisher() + "' AND YEAR = " +
                            book.getYear() +
                            ";"
            );
            if (!results.next()) {
                results.close();
            } else {
                int id = results.getInt(1);
                results.close();
                return id;
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public static int InsertBook (Book book){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("ROLLBACK;");
            command.executeUpdate(
                    "INSERT INTO L_BOOK (TITLE, AUTHOR, PUBLISHER, YEAR) VALUES ('" +
                    book.getTitle() + "','"+
                    book.getAuthor() + "','" +
                    book.getPublisher() + "'," +
                    book.getYear()+
                    ");"
            );
            command.execute("commit;");
            int id = FindIdenticalBook(book);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static BookList LookUpBook (Book book, int limit, String sortBy, boolean asc){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            BookList returnList = new BookList();
            String sqlStatement = "SELECT * FROM L_BOOK WHERE ";
            boolean isFirstCritera = true;
            if(book.getBookId() != 0){
                sqlStatement += "ID = " + book.getBookId();
                isFirstCritera = false;
            }
            if(!book.getTitle().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                }
                isFirstCritera = false;
                sqlStatement += "TITLE LIKE '%" + book.getTitle() + "%'";
            }
            if(!book.getAuthor().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                }
                isFirstCritera = false;
                sqlStatement += "AUTHOR LIKE '%" + book.getAuthor() + "%'";
            }
            if(!book.getPublisher().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                }
                isFirstCritera = false;
                sqlStatement += "PUBLISHER LIKE '%" + book.getPublisher() + "%'";
            }
            if(!book.getYear().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                }
                isFirstCritera = false;
                sqlStatement += "YEAR = " + book.getYear();
            }
            if(book.getAvailable() != null){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                }
                sqlStatement += "AVAILABLE = " + book.getAvailable();
            }
            if(!sortBy.equals("")){
                sqlStatement += " ORDER BY " + sortBy;
                if(asc){
                    sqlStatement += " ASC";
                }else{
                    sqlStatement += " DESC";
                }
            }

            //Remove "WHERE" in the statement if no condition
            String originalSqlStatement = "SELECT * FROM L_BOOK WHERE ";
            if (sqlStatement.equals(originalSqlStatement)){
                sqlStatement = "SELECT * FROM L_BOOK";
            }

            sqlStatement = sqlStatement + ";";

            ResultSet results = command.executeQuery(sqlStatement);
            int count = 0;
            while (results.next() && (count < limit || limit == 0)) {
                count++;
                Book returnBook = new Book();
                returnBook.setTitle(results.getString("TITLE"));
                returnBook.setAuthor(results.getString("AUTHOR"));
                returnBook.setPublisher(results.getString("PUBLISHER"));
                returnBook.setYear(results.getString("YEAR"));
                returnBook.setAvailable(null);

                returnList.AddResult(returnBook);
            }
            returnList.setFoundBooks(count);
            results.close();
            return returnList;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int LoanBook (int id, boolean isTransaction){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            if (!isTransaction){
                command.execute("ROLLBACK;");
            };
            ResultSet results = command.executeQuery(
                    "SELECT AVAILABLE FROM L_BOOK WHERE ID = " +
                            id +
                            ";"
            );

            if (!results.next()) {
                results.close();
                return 10;
            }
            else if (!results.getBoolean(1)){
                return 15;
            }
            else {
                results.close();
            }

            command.execute("UPDATE L_BOOK SET AVAILABLE = 0 WHERE ID = " + id + ";");
            if (!isTransaction){
                command.execute("commit;");
            }
            return 20;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int ReturnBook (int id, boolean isTransaction){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            if (!isTransaction){
                command.execute("ROLLBACK;");
            };
            ResultSet results = command.executeQuery(
                    "SELECT AVAILABLE FROM L_BOOK WHERE ID = " +
                            id +
                            ";"
            );
            if (!results.next()) {
                results.close();
                return 10;
            } else {
                if (results.getBoolean(1)){
                    return 15;
                }
                results.close();
            }
            command.execute("UPDATE L_BOOK SET AVAILABLE = 1 WHERE ID = " + id + ";");
            if (!isTransaction){
                command.execute("ROLLBACK;");
            };
            return 20;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean DeleteBook (int id){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("ROLLBACK;");
            ResultSet results = command.executeQuery(
                    "SELECT * FROM L_BOOK WHERE ID = " +
                            id +
                            ";"
            );
            if (!results.next()) {
                results.close();
                return false;
            } else {
                results.close();
            }
            command.execute("DELETE FROM L_BOOK WHERE ID = " + id + ";");
            command.execute("commit;");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int InsertTransaction (String token){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery("SELECT connection_id();");
            int transactionId = 0;
            if (!results.next()) {
                results.close();
            }
            else{
                transactionId = results.getInt(1);
            }
            results.close();
            if(IsTransactionIdFound(transactionId,"") == 10){
                command.execute("ROLLBACK;");
                command.execute("INSERT INTO L_TRANSACTION VALUES(connection_id(),'"+token+"',NOW());");
                command.execute("START TRANSACTION;");
                return transactionId;
            }else{
                return 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean UpdateTransaction (Transaction transaction){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            int status = 0;
            if (transaction.getAction().toUpperCase().equals("LOAN")){
                status = LoanBook(transaction.getBookId(), true);
            }else if(transaction.getAction().toUpperCase().equals("RETURN")){
                status = ReturnBook(transaction.getBookId(), true);
            }
            if(status != 20){
                return false;
            }
            command.execute("UPDATE L_TRANSACTION SET CREATE_TIME=NOW() WHERE TRANSACTION_ID = connection_id();");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean CommitTransaction (){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("COMMIT;");
            command.execute("DELETE FROM L_TRANSACTION WHERE TRANSACTION_ID = connection_id();");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean CancelTransaction (){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("ROLLBACK;");
            command.execute("DELETE FROM L_TRANSACTION WHERE TRANSACTION_ID = connection_id();");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int IsTransactionIdFound (int transactionId, String token){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery("SELECT connection_id();");
            if (!results.next()) {
                results.close();
            }
            else{
                int correctId = results.getInt(1);
                if(correctId != transactionId){
                    return 5;
                }
            }
            results = command.executeQuery(
                    "SELECT * FROM L_TRANSACTION WHERE TRANSACTION_ID = "+
                            transactionId +
                            ";");
            if (!results.next()) {
                return 10;
            }
            results = command.executeQuery(
                    "SELECT * FROM L_TRANSACTION WHERE TRANSACTION_ID = "+
                            transactionId + " AND TOKEN = '" + token + "'" +
                            ";");
            if (!results.next()) {
                results.close();
                return 15;
            }
            results.close();
            return 20;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}

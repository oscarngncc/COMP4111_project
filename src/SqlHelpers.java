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
            command.execute("INSERT INTO L_TOKEN (TOKEN) VALUES ('" + token + "');");
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
            command.execute("DELETE FROM L_TOKEN WHERE TOKEN = '" + token + "';");
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
                ResultSet results = command.executeQuery("SELECT * FROM L_TOKEN WHERE TOKEN = '" + token + "';");
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
            command.execute(
                    "INSERT INTO L_BOOK (TITLE, AUTHOR, PUBLISHER, YEAR) VALUES ('" +
                    book.getTitle() + "','"+
                    book.getAuthor() + "','" +
                    book.getPublisher() + "'," +
                    book.getYear()+
                    ");"
            );
            int id = FindIdenticalBook (book);
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
                    isFirstCritera = false;
                }
                sqlStatement += "TITLE LIKE '%" + book.getTitle() + "%'";
            }
            if(!book.getAuthor().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                    isFirstCritera = false;
                }
                sqlStatement += "AUTHOR LIKE '%" + book.getAuthor() + "%'";
            }
            if(!book.getPublisher().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                    isFirstCritera = false;
                }
                sqlStatement += "PUBLISHER LIKE '%" + book.getPublisher() + "%'";
            }
            if(!book.getYear().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                    isFirstCritera = false;
                }
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
            sqlStatement = sqlStatement + ";";
            System.out.println("The SQL lookUP statement is:\n" + sqlStatement );
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

    public static int LoanBook (int id){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery(
                    "SELECT AVAILABLE FROM L_BOOK WHERE ID = " +
                            id +
                            ";"
            );
            if (!results.next()) {
                results.close();
                return 10;
            } else {
                if (!results.getBoolean(1)){
                    return 15;
                }
                results.close();
            }
            command.execute("UPDATE L_BOOK SET AVAILABLE = 0 WHERE ID = " + id + ";");
            return 20;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int ReturnBook (int id){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
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
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean InsertTransaction (String transactionId){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("INSERT INTO L_TRANSACTION (TRANSACTION_ID, CREATE_TIME) VALUES ('" + transactionId + "', NOW());");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean UpdateTransaction (Transaction transaction){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute(
                    "UPDATE L_TRANSACTION SET BOOK_ID = " +
                            transaction.getBookId()+", ACTION = '" +
                            transaction.getAction() + "' WHERE TRANSACTION_ID = '" +
                            transaction.getTransactionId() +"'"+
                            ";"
            );
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean CommitTransaction (int transactionId){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery(
                    "SELECT BOOK_ID, ACTION FROM L_TRANSACTION WHERE TRANSACTION_ID = '" +
                            transactionId +
                            "';"
            );
            int status = 0;
            results.next();
            int bookId = results.getInt("BOOK_ID");
            String action = results.getString("ACTION");
            results.close();
            if (action.equals("loan")){
                status = LoanBook(bookId);
            }else if(action.equals("return")){
                status = ReturnBook(bookId);
            }
            if(status == 20){
                CancelTransaction(transactionId);
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean CancelTransaction (int transactionId){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("DELETE FROM L_TRANSACTION WHERE TRANSACTION_ID = '" + transactionId + "';");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean IsTransactionIdFound (String transactionId){
        try{
            if(transactionId != null) {
                Connection connection = SqlSingleton.getConnection();
                Statement command = connection.createStatement();
                ResultSet results = command.executeQuery("SELECT * FROM L_TRANSACTION WHERE TRANSACTION_ID = '" + transactionId + "';");
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
}

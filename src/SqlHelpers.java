import json.Book;
import json.BookList;
import json.Transaction;

import java.awt.desktop.SystemEventListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlHelpers {
    /**
     * Method to check the user is in DB or not
     * @param username username of the user
     * @param password password of the user
     * @return true if the user can be found, otherwise false
     */
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
    /**
     * Method to insert token to DB
     * @param token token string
     * @return true if the insert is success, otherwise false
     */
    public static boolean InsertToken (String token){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            command.execute("INSERT INTO L_TOKEN VALUES ('" + token + "');");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Method to delete token from DB
     * @param token token string
     * @return true if the delete is success, otherwise false
     */
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
    /**
     * Method to check the token is in DB or not
     * @param token token of the user
     * @return true if the token can be found, otherwise false
     */
    public static boolean IsTokenFound (String token){
        try{
            if(token != null) {
                Connection connection = SqlSingleton.getConnection();
                Statement command = connection.createStatement();
                ResultSet results = command.executeQuery(
                        "SELECT * FROM L_TOKEN WHERE TOKEN = '" + token + "';"
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

    /**
     * Method to check there is a token associated the user in DB
     * @param userId userId of the user like "001"
     * @return true if the token can be found, otherwise false
     */
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

    /**
     * Method to check there is an identical book in DB
     * @param book book info
     * @return the book id if the book can be found, otherwise 0
     */
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
    /**
     * Method to insert book into DB
     * @param book book info
     * @return id if the insert is success, otherwise 0
    */
    public static int InsertBook (Book book){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
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

    /**
     * Method to look up book in DB
     * @param book book info
     * @param limit limit of the no. of results
     * @param sortBy the column of the result should be sorted by
     * @param asc indicate the result is in which order
     * @return BookList containing all results matching the criteria
     */
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

            if (isFirstCritera){
                sqlStatement = "SELECT * FROM L_BOOK";
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
    /**
     * Method to Loan book in DB
     * @param id book id
     * @return 10 if book not found; 15 if book is in conflict status; 20 if success
     */
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
            }
            else if (!results.getBoolean(1)){
                return 15;
            }
            else {
                results.close();
            }
            command.execute("UPDATE L_BOOK SET AVAILABLE = 0 WHERE ID = " + id + ";");
            command.execute("commit;");
            return 20;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * Method to return book in DB
     * @param id book id
     * @return 10 if book not found; 15 if book is in conflict status; 20 if success
     */
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
                if (results.getBoolean(1)) {
                    return 15;
                }
                results.close();
            }
            command.execute("UPDATE L_BOOK SET AVAILABLE = 1 WHERE ID = " + id + ";");
            command.execute("commit;");
            return 20;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * Method to delete book in DB
     * @param id book id
     * @return true if success, otherwise false
     */
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
            command.execute("commit;");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Method to start transaction in DB
     * @param token user's token
     * @return transaction id, otherwise 0
     */
    public static int InsertTransaction (String token){
        try {
            Connection connection = SqlSingleton.getTransactionConnection(token);
            if(!connection.equals(null)){
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
                connection.setAutoCommit(false);
                return transactionId;
            }else{
                return 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    /**
     * Method to update transaction and perform action in DB
     * @param transaction transaction info
     * @return true if action can be performed, otherwise false
     */
    public static boolean UpdateTransaction (Transaction transaction){
        try {
            Connection connection = SqlSingleton.getTransactionConnection(transaction.getTransactionId());
            if(!connection.equals(null)){
                Statement command = connection.createStatement();
                int status = 0;
                if (transaction.getAction().toUpperCase().equals("LOAN")){
                    command.execute("UPDATE L_BOOK SET AVAILABLE = 0 WHERE ID = " + transaction.getBookId() + ";");
                }else if(transaction.getAction().toUpperCase().equals("RETURN")){
                    command.execute("UPDATE L_BOOK SET AVAILABLE = 1 WHERE ID = " + transaction.getBookId() + ";");
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Method to commit transaction in DB
     * @return true if action can be committed, otherwise false
     */
    public static boolean CommitTransaction (Transaction transaction){
        try {
            Connection connection = SqlSingleton.getTransactionConnection(transaction.getTransactionId());
            if(!connection.equals(null)){
                Statement command = connection.createStatement();
                connection.commit();
                command.execute("DELETE FROM L_TRANSACTION WHERE TRANSACTION_ID = connection_id();");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Method to rollback transaction in DB
     * @return true if action can be rollbacked, otherwise false
     */
    public static boolean CancelTransaction (Transaction transaction){
        try {
            Connection connection = SqlSingleton.getTransactionConnection(transaction.getTransactionId());
            if(!connection.equals(null)){
                Statement command = connection.createStatement();
                connection.rollback();
                command.execute("DELETE FROM L_TRANSACTION WHERE TRANSACTION_ID = connection_id();");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    /**
     * Method to check the transaction id is valid or not
     * @param transactionId user's transactionId
     * @param token user's token
     * @return 5 if transactionId not valid; 10 if transactionId not exist; 15 if transactionId not matching the token; 20 if everything is ok
     */
    public static boolean IsTransactionIdFound (int transactionId, String token){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery(
                    "SELECT * FROM L_TRANSACTION WHERE TRANSACTION_ID = "+
                            transactionId + " AND TOKEN = '" + token + "'" +
                            ";");
            if (!results.next()) {
                results.close();
                return false;
            }
            results.close();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}

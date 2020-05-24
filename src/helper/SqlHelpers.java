package helper;

import json.Book;
import json.BookList;
import json.Transaction;

import java.awt.desktop.SystemEventListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.*;


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

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM L_USER WHERE USERNAME = ? AND PASSWORD =?");
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet results = stmt.executeQuery();

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
    public static boolean InsertToken (String token, String username){
        try {
            Connection connection = SqlSingleton.getConnection();
            Statement command = connection.createStatement();

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO L_TOKEN VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, token);
            int affectedRowNo = stmt.executeUpdate();

            if(affectedRowNo > 0){
                return true;
            }else{
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
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

            PreparedStatement stmt = connection.prepareStatement("DELETE FROM L_TOKEN WHERE TOKEN = ?");
            stmt.setString(1, token);
            int affectedRowNo = stmt.executeUpdate();
            if( affectedRowNo > 0){
                return true;
            }else{
                return false;
            }
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

                PreparedStatement stmt = connection.prepareStatement("SELECT * FROM L_TOKEN WHERE TOKEN = ?");
                stmt.setString(1, token);
                ResultSet results = stmt.executeQuery();

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

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM L_TOKEN WHERE USERNAME = ?" );
            stmt.setString(1, userId);
            ResultSet results = stmt.executeQuery();

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

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM L_BOOK WHERE TITLE=?" );
            stmt.setString(1, book.getTitle());
            ResultSet results = stmt.executeQuery();

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

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO L_BOOK (TITLE, AUTHOR, PUBLISHER, YEAR) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, book.getTitle());
            stmt.setString(2, book.getAuthor());
            stmt.setString(3, book.getPublisher());
            stmt.setString(4, book.getYear());
            stmt.execute();
            ResultSet results = stmt.getGeneratedKeys();

            if (!results.next()) {
                results.close();
            } else {
                int id = results.getInt(1);
                results.close();
                return id;
            }
        } catch (SQLException e) {
            return 0;
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
                sqlStatement += "ID = ?";
                isFirstCritera = false;
            }
            if(!book.getTitle().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                }
                isFirstCritera = false;
                sqlStatement += "TITLE LIKE ?";
            }
            if(!book.getAuthor().equals("")){
                if(!isFirstCritera){
                    sqlStatement += " AND ";
                }
                isFirstCritera = false;
                sqlStatement += "AUTHOR LIKE ?";
            }

            /* If no requirement */
            if (isFirstCritera){
                sqlStatement = "SELECT * FROM L_BOOK";
            }

            if(!sortBy.equals("")){
                sqlStatement += " ORDER BY " + sortBy + " ";
                if(asc){
                    sqlStatement += " ASC";
                }else{
                    sqlStatement += " DESC";
                }
            }

            int start = 1;
            PreparedStatement stmt = connection.prepareStatement(sqlStatement);
            String Options[] = new String[]{ "ID", "TITLE", "AUTHOR"};
            String Values[] = new String[]{ "",  "%" + book.getTitle() + "%", "%" + book.getAuthor() + "%"};


            for ( int i = 0; i < Options.length; i++ ){
                if ( i == 0 && sqlStatement.contains("ID") ){
                    stmt.setInt(start, book.getBookId());
                    start++;
                }
                else if (sqlStatement.contains(Options[i])){
                    stmt.setString(start, Values[i] );
                    start++;
                }
            }
            ResultSet results = stmt.executeQuery();


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
    public static int PutBookAction (int id, boolean action) {
        try {
            Connection connection = SqlSingleton.getConnection();

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO L_BOOK_LOCK VALUE (? , 0);");
            stmt.setInt(1, id);
            stmt.executeUpdate();

            stmt = connection.prepareStatement("SELECT AVAILABLE FROM L_BOOK WHERE ID = ?");
            stmt.setInt(1, id);
            ResultSet results = stmt.executeQuery();

            if (!results.next()) {
                stmt = connection.prepareStatement("DELETE FROM L_BOOK_LOCK WHERE ID = ?;");
                stmt.setInt(1, id);
                stmt.executeUpdate();
                results.close();
                return 10;
            }
            else if (results.getBoolean(1) == action){
                stmt = connection.prepareStatement("DELETE FROM L_BOOK_LOCK WHERE ID = ?;");
                stmt.setInt(1, id);
                stmt.executeUpdate();
                results.close();
                return 15;
            }
            else {
                results.close();
            }

            stmt = connection.prepareStatement("UPDATE L_BOOK SET AVAILABLE = ? WHERE ID = ?");
            stmt.setBoolean(1, action);
            stmt.setInt(2, id);
            int affectedRowNo = stmt.executeUpdate();

            stmt = connection.prepareStatement("DELETE FROM L_BOOK_LOCK WHERE ID = ?;");
            stmt.setInt(1, id);
            stmt.executeUpdate();

            if( affectedRowNo > 0){
                return 20;
            }else{
                return 15;
            }

        } catch (SQLException e) {
            return 15;
        }
    }

    /**
     * Method to delete book in DB
     * @param id book id
     * @return true if success, otherwise false
     */
    public static boolean DeleteBook (int id){
        try {
            Connection connection = SqlSingleton.getConnection();

            PreparedStatement stmt = connection.prepareStatement("DELETE FROM L_BOOK WHERE ID = ?");
            stmt.setInt(1, id);
            int affectedRowNo = stmt.executeUpdate();
            if( affectedRowNo > 0){
                return true;
            }else{
                return false;
            }

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
    public static int GetTransactionId (Connection connection, String token){
        int id = 0;
        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO L_TRANSACTION VALUES(connection_id(),?,NOW());");
            stmt.setString(1, token);
            stmt.executeUpdate();

            Statement command = connection.createStatement();
            ResultSet results = command.executeQuery("SELECT connection_id();");
            if (results.next()) {
                id = results.getInt(1);
            }
            results.close();
        } catch (SQLException e) {

        }
        return id;
    }

    public static int InsertTransaction (String token){
        try {
            return SqlSingleton.getTransactionConnectionId(token);
        } catch (SQLException e) {

        }
        return 0;
    }
    /**
     * Method to update transaction and perform action in DB
     * @param transaction transaction info
     * @return true if action can be performed, otherwise false
     */
    public static boolean UpdateTransaction (Transaction transaction){
        Connection connection = SqlSingleton.getTransactionConnection(transaction.getTransactionId());
        if(!connection.equals(null)){
            if (transaction.getAction().toUpperCase().equals("LOAN")){
                return putTransactionAction(transaction.getBookId(), false,connection);
            }else if(transaction.getAction().toUpperCase().equals("RETURN")){
                return putTransactionAction(transaction.getBookId(),true,connection);
            }
        }
        return false;
    }

    private static boolean putTransactionAction (int id, boolean bookAction, Connection connection ){
        try {
            int count = 0;
            int timeout = 1000;
            boolean isGetAccess = false;

            while(count < 3 && !isGetAccess) {
                try {
                    PreparedStatement stmt = connection.prepareStatement("SELECT * FROM L_BOOK_LOCK WHERE ID = ? AND TRANSACTION_ID = connection_id();");
                    stmt.setInt(1, id);
                    ResultSet results = stmt.executeQuery();

                    if (results.next()) {
                        results.close();
                        isGetAccess = true;
                    } else {
                        results.close();
                        stmt = connection.prepareStatement("INSERT INTO L_BOOK_LOCK VALUE (? ,connection_id());");
                        stmt.setInt(1, id);
                        int affectedRowNo = stmt.executeUpdate();
                        if (affectedRowNo > 0) {
                            isGetAccess = true;
                        }else{
                            Thread.sleep(timeout);
                            count++;
                        }
                    }
                }catch (SQLException e){
                    Thread.sleep(timeout);
                    count++;
                }
            }
            if(!isGetAccess){
                return false;
            }
            PreparedStatement stmt = connection.prepareStatement("UPDATE L_BOOK SET AVAILABLE = ? WHERE ID = (SELECT ID FROM L_BOOK_LOCK WHERE ID = ? AND TRANSACTION_ID = connection_id());");
            stmt.setBoolean(1,bookAction);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            return true;
        } catch (SQLException | InterruptedException e) {
            return false;
        }
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
                command.execute("DELETE FROM L_BOOK_LOCK WHERE TRANSACTION_ID = connection_id();");
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
                command.execute("DELETE FROM L_BOOK_LOCK WHERE TRANSACTION_ID = connection_id();");
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

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM L_TRANSACTION WHERE TRANSACTION_ID = ? AND TOKEN = ?");
            stmt.setInt(1, transactionId);
            stmt.setString(2, token);
            ResultSet results = stmt.executeQuery();

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

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
                return true;
            }
        }catch(SQLException e){
            e.printStackTrace();
            return false;
        }
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

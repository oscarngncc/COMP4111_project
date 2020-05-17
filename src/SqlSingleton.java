import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This the class of Sql Singleton
 *
 */
public class SqlSingleton {
    /**
     * Singleton object
     */
    private static SqlSingleton obj;
    /**
     * Default connection config
     */
    private static String connectionString = "jdbc:mysql://localhost:3333/lbm";
    private static String username = "sqlUser";
    private static String password = "sqlUserPwd10000";
    /**
     * Single connection
     */
    private Connection connection;
    private static Map<Integer,Connection> connectionPool= new HashMap<Integer,Connection>();;
    // private constructor to force use of
    // getConnection() to create Singleton object
    private SqlSingleton(){

    }
    /**
     * Method to set MySQL url
     * @param connectionStringInput the MySQL url input
     */
    public static void setConnection(String connectionStringInput) {
        connectionString = connectionStringInput;
    }
    /**
     * Method to create and return the single connection
     * @return connection
     */
    public static Connection getConnection() throws SQLException {
        if (obj==null)
        {
                if (obj==null) {
                    obj = new SqlSingleton();
                }
        }
        if(obj.connection == null){
            try {
                obj.connection = DriverManager.getConnection(connectionString, username, password);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if(!obj.connection.isValid(3)){
            obj.connection = DriverManager.getConnection(connectionString, username, password);
        }
        obj.connection.beginRequest();
        return obj.connection;
    }
    /**
     * Method to find an available connection in connection pool
     * @return connection
     */
    public static Connection getTransactionConnection(String token) throws SQLException {
        Connection tranConnection = DriverManager.getConnection(connectionString, username, password);
        tranConnection.setAutoCommit(false);
        Statement command = tranConnection .createStatement();
        command.execute("INSERT INTO L_TRANSACTION VALUES(connection_id(),'"+token+"',NOW());");
        ResultSet results = command.executeQuery("SELECT connection_id();");
        if (results.next()) {
            int id = results.getInt(1);
            connectionPool.put(id, tranConnection);
        }
        results.close();
        return tranConnection;
    }
    /**
     * Method to find the occupied connection in connection pool
     * @return connection
     */
    public static Connection getTransactionConnection(int id) throws SQLException {
        if (connectionPool.containsKey(id)) {
            return connectionPool.get(id);
        } else {
            return null;
        }
    }
    /**
     * Method to exit connection
     */
    public static void exitConnection() throws SQLException {
        if (obj!=null)
        {
            try{
                obj.connection.close();
                obj = null;
            } catch(Exception e) {
                System.out.println(e);
            }
        }
        for (Map.Entry<Integer, Connection> entry : connectionPool.entrySet()) {
            entry.getValue().close();
        }
    }
}

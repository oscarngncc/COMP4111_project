import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
    private static String connectionString = "jdbc:mysql://localhost:3306/lbm";
    private static String username = "sqlUser";
    private static String password = "sqlUserPwd10000";
    /**
     * Single connection
     */
    private Connection connection;
    // private constructor to force use of
    // getConnection() to create Singleton object
    private SqlSingleton() {

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
            // To make thread safe
                // check again as multiple threads
                // can reach above step
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
        return obj.connection;
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
    }
}

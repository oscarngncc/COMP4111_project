import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlSingleton {
    private static SqlSingleton obj;

    static private final String connectionString = "jdbc:mysql://localhost:3306/lbm";
    static private final String username = "root";
    static private final String password = "MySQLCOMP4111";

    private Connection connection;
    // private constructor to force use of
    // getInstance() to create Singleton object
    private SqlSingleton() {

    }

    public static Connection getConnection() throws SQLException {
        if (obj==null)
        {
            // To make thread safe
            synchronized (SqlSingleton.class)
            {
                // check again as multiple threads
                // can reach above step
                if (obj==null) {
                    obj = new SqlSingleton();
                }
            }
        }
        if(obj.connection == null){
            try {
                obj.connection = DriverManager.getConnection(connectionString, username, password);
                obj.connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return obj.connection;
    }

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

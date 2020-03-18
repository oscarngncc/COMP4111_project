import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlSingleton {
    private static SqlSingleton obj;

    private static String username = DBConnectionKeys.username;
    private static String password = DBConnectionKeys.password;
    private static String connectionString = DBConnectionKeys.address;


    private Connection connection;
    // private constructor to force use of
    // getInstance() to create Singleton object
    private SqlSingleton() {

    }

    public static Connection getConnection()
    {
        if (obj==null)
        {
            // To make thread safe
            synchronized (SqlSingleton.class)
            {
                // check again as multiple threads
                // can reach above step
                if (obj==null) {
                    obj = new SqlSingleton();
                    try {
                        obj.connection = DriverManager.getConnection(connectionString, username, password);
                        obj.connection.setAutoCommit(true);
                        if (obj.connection.isValid(1000)){ System.out.println("Connection is Valid");}
                        else { System.out.println("Connection inValid"); }
                    } catch (SQLException e) {

                        e.printStackTrace();
                    }
                }
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

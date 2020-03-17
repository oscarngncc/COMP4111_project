import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlSingleton {
    private static SqlSingleton obj;

    private static String username = "sqlUser";
    private static String password = "sqlUserPwd10000";

    private static String connectionString = "jdbc:mysql://ec2-18-162-194-78.ap-east-1.compute.amazonaws.com/LBM";

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

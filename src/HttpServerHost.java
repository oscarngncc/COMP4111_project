
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
/**
 * This is the main class: HttpServerHost. It is used to set the configuration of socket, register handlers and start the web server.
 *
 */
public class HttpServerHost {

    public static void main(String[] args) throws Exception {
        // Get the MySQL connection URL if it is provided
        if(args.length > 0)
            SqlSingleton.setConnection(args[0]);

        //Config Socket
        int port = 8080;

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        //Web Server and register handlers
        final HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Test/1.1")
                .setSocketConfig(socketConfig)
                .setExceptionLogger(new StdErrorExceptionLogger())
                .registerHandler("/BookManagementService/login*", new HttpHandlers.HttpLoginHandler())
                .registerHandler("/BookManagementService/logout*", new HttpHandlers.HttpLogoutHandler())
                .registerHandler("/BookManagementService/books*", new HttpHandlers.HttpBookHandler())
                .registerHandler("/BookManagementService/transaction*", new HttpHandlers.HttpTransactionHandler())
                .create();

        //Start server
        server.start();
        server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        //Server Shutdown handling
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    SqlSingleton.exitConnection();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                server.shutdown(5, TimeUnit.SECONDS);
            }
        });
    }
    /**
     * This the class of a Error Exception Logger.
     *
     */
    static class StdErrorExceptionLogger implements ExceptionLogger {

        @Override
        public void log(final Exception ex) {
            if (ex instanceof SocketTimeoutException) {
                System.err.println("Connection timed out");
            } else if (ex instanceof ConnectionClosedException) {
                System.err.println(ex.getMessage());
            } else {
                ex.printStackTrace();
            }
        }

    }
}

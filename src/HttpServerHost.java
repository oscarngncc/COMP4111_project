
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import handler.HttpBookHandler;
import handler.HttpLoginHandler;
import handler.HttpLogoutHandler;
import handler.HttpTransactionHandler;
import helper.SqlSingleton;
import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.protocol.HttpAsyncService;
import org.apache.http.pool.PoolEntry;

/**
 * This is the main class: HttpServerHost. It is used to set the configuration of socket, register handlers and start the web server.
 *
 */
public class HttpServerHost {

    public static void main(String[] args) throws Exception {
        // Get the MySQL connection URL if it is provided


        //Config Socket
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            }catch(NumberFormatException e){
            }
        }

        if(args.length > 1) {
            SqlSingleton.setConnection(args[1]);
        }

        if(args.length == 4) {
            SqlSingleton.setConnection(args[1], args[2], args[3]);
        }

        final IOReactorConfig config = IOReactorConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();
        //Web Server and register handlers
        final HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Test/1.1")
                .setIOReactorConfig(config)
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .registerHandler("/BookManagementService/login*", new HttpLoginHandler())
                .registerHandler("/BookManagementService/logout*", new HttpLogoutHandler())
                .registerHandler("/BookManagementService/books*", new HttpBookHandler())
                .registerHandler("/BookManagementService/transaction*", new HttpTransactionHandler())
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
}

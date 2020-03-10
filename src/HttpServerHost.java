
import java.net.SocketTimeoutException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionClosedException;
import org.apache.http.ExceptionLogger;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;

public class HttpServerHost {

    public static void main(String[] args) throws Exception {

        int port = 8080;

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

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

        server.start();
        server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

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

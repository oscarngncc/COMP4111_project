import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {
    static Thread serverThread;

    private HttpProcessor httpproc;
    private HttpRequestExecutor httpexecutor;
    HttpCoreContext coreContext;
    HttpHost host;
    DefaultBHttpClientConnection conn;
    ConnectionReuseStrategy connStrategy;

    int userNum;
    String userToken;
    String responseBody;

    @BeforeAll
    static public void init(){
        try {
            serverThread = new Thread(() -> {
                try {
                    System.out.println("Server started");
                    String[] args = {};
                    HttpServerHost.main(args);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            });
            serverThread.start();
        } catch (Exception e ){ System.out.println("Something is wrong starting the server");   e.printStackTrace();  }
    }


    /******* User00009 Login *********/
    @BeforeEach
    public void initEach() throws IOException, HttpException {

        /** HTTP Thing setup*/
        httpproc = HttpProcessorBuilder.create()
                .add( new RequestContent())
                .add( new RequestTargetHost())
                .add( new RequestConnControl())
                .add( new RequestUserAgent("Test/1.1"))
                .add(new RequestExpectContinue(true)).build();
        httpexecutor = new HttpRequestExecutor();
        coreContext = HttpCoreContext.create();
        host = new HttpHost("localhost", 8080);
        coreContext.setTargetHost(host);
        conn = new DefaultBHttpClientConnection(8 * 1024);
        connStrategy = DefaultConnectionReuseStrategy.INSTANCE;

        /** A user is expected to login first*/
        if (!conn.isOpen()){
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket);
        }
        //userNum = (int)(Math.random() * 8) + 1;
        userNum = 9;
        String username = "user00" + Integer.toString(userNum) ;
        String password = "pass00" +  Integer.toString(userNum);
        String jsonBody = "{\"Username\": \"" + username + "\",\"Password\": \"" + password + "\"}";
        HttpEntity requestEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);

        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", "/BookManagementService/login", HttpVersion.HTTP_1_1);
        request.setEntity(requestEntity);

        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        var arr = responseBody.split("\"");
        userToken = arr[3];

        conn.close();
    }



    /******* LogOut *********/
    @AfterEach
    public void closeEach() throws IOException, HttpException {
        if (!conn.isOpen()){
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket);
        }

        HttpRequest request = new BasicHttpRequest("GET",
                "/BookManagementService/logout?token=" + userToken,
                HttpVersion.HTTP_1_1);
        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        conn.close();
    }


    @Test
    public void test() throws IOException, HttpException {

    }


}
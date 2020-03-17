
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.*;


import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationTest {

    static Thread serverThread;

    private HttpProcessor httpproc;
    private HttpRequestExecutor httpexecutor;
    HttpCoreContext coreContext;
    HttpHost host;
    DefaultBHttpClientConnection conn;
    ConnectionReuseStrategy connStrategy;


    /** Saving the responseBody before closing it
     *  This variable will be updated in Login() if success (200) */
    String responseBody;


    @BeforeAll
    static public void init(){
        serverThread = new Thread(()->{
            try {
                System.out.println("Server started");
                String[] args = {};
                HttpServerHost.main(args);
            } catch (Exception e){ e.printStackTrace(); return; }
        });
        serverThread.start();
    }


    @BeforeEach
    public void initEach() throws IOException {
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

        if (!conn.isOpen()){
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket);
        }
    }


    @AfterEach
    public void closeEach() throws IOException {
        if (conn.isOpen()){conn.close();}
    }


    /** Return the Response, also saving the body beforehand **/
    private HttpResponse Login(String method, String username, String password) throws  IOException, HttpException {

        if (!conn.isOpen()){
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket);
        }

        String jsonBody = "{\"Username\": \"" + username + "\",\"Password\": \"" + password + "\"}";
        HttpEntity requestEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);

        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest(method, "/BookManagementService/login", HttpVersion.HTTP_1_1);
        request.setEntity(requestEntity);

        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ){
            responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        }

        conn.close();
        return response;
    }


    private HttpResponse Logout(String token) throws IOException, HttpException {

        if (!conn.isOpen()){
            Socket socket = new Socket(host.getHostName(), host.getPort());
            conn.bind(socket);
        }

        HttpRequest request = new BasicHttpRequest("GET",
                "/BookManagementService/logout?token=" + token,
                HttpVersion.HTTP_1_1);
        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        conn.close();
        return response;
    }



    @Test
    public void checkGetRequest() throws IOException, HttpException {

        HttpRequest getRequest = new BasicHttpRequest("GET", "/BookManagementService/login", HttpVersion.HTTP_1_1);
        httpexecutor.preProcess(getRequest, httpproc, coreContext);
        HttpResponse getResponse = httpexecutor.execute(getRequest, conn, coreContext);
        httpexecutor.postProcess(getResponse, httpproc, coreContext);
        String responseBody = EntityUtils.toString(getResponse.getEntity(), StandardCharsets.UTF_8);

        //501
        assertEquals( HttpStatus.SC_NOT_IMPLEMENTED ,getResponse.getStatusLine().getStatusCode() );
        assertEquals("GET method not supported", responseBody);
    }

    @Test
    public void checkIncorrectLogin() throws IOException, HttpException {
        //400
        HttpResponse response = Login("POST", "user00001", "passwd00007");
        assertEquals( HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode() );

        HttpResponse response2 = Login("POST", "user00005", "");
        assertEquals( HttpStatus.SC_BAD_REQUEST, response2.getStatusLine().getStatusCode() );

        HttpResponse response3 = Login("POST", "", "");
        assertEquals( HttpStatus.SC_BAD_REQUEST, response3.getStatusLine().getStatusCode() );

        HttpResponse response4 = Login("POST", "temp~!@$5", "#12rcd@4erea\'");
        assertEquals( HttpStatus.SC_BAD_REQUEST, response3.getStatusLine().getStatusCode() );
    }


    @Test
    public void checkSuccessLoginLogOut() throws IOException, HttpException {

        HttpResponse response = Login("POST", "user00001", "passwd00001");

        //200
        assertEquals( HttpStatus.SC_OK, response.getStatusLine().getStatusCode() );
        assertTrue(responseBody.contains("Token"));

        var arr = responseBody.split("\"");
        String token = arr[3];
        HttpResponse logoutResponse = Logout(token);

        //200
        assertEquals( HttpStatus.SC_OK, logoutResponse.getStatusLine().getStatusCode() );
    }




    @Test
    public void checkDuplicateLogin() throws IOException, HttpException {

        //Login
        HttpResponse response = Login("POST", "user00010", "passwd00010");

        //200
        assertEquals( HttpStatus.SC_OK, response.getStatusLine().getStatusCode() );
        assertTrue(responseBody.contains("Token"));

        var arr = responseBody.split("\"");
        String token = arr[3];

        //409
        HttpResponse response2 = Login("POST", "user00010", "passwd00010");
        assertEquals(HttpStatus.SC_CONFLICT, response2.getStatusLine().getStatusCode());

        //200
        HttpResponse logoutResponse = Logout(token);
        assertEquals( HttpStatus.SC_OK, logoutResponse.getStatusLine().getStatusCode() );
    }



    @Test
    void checkIncorrectLogout() throws IOException, HttpException {
        //400
        String wrongToken = "I am a wrong Token";
        HttpResponse logoutResponse1 = Logout(wrongToken);
        assertEquals( HttpStatus.SC_BAD_REQUEST, logoutResponse1.getStatusLine().getStatusCode() );

        //400
        String wrongToken2 = "This 1 is a Stress Teesssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssss" +
                "ssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssT";
        HttpResponse logoutResponse2 = Logout(wrongToken2);
        assertEquals( HttpStatus.SC_BAD_REQUEST, logoutResponse2.getStatusLine().getStatusCode() );
    }




}
import org.apache.http.*;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.nio.DefaultHttpClientIODispatch;
import org.apache.http.impl.nio.pool.BasicNIOConnPool;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestExecutor;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentTest {

    final String LOGIN = "LOGIN";
    final String LOGOUT = "LOGOUT";



    static Thread serverThread;
    static Thread clientThread;

    private static HttpProcessor httpproc;
    private static BasicNIOConnPool pool;
    private static ConnectingIOReactor ioReactor;

    //Values
    private boolean hasError = false;
    private List<String> tokens = new ArrayList<>();
    private List<Integer> statusCodes = new ArrayList<>();


    @BeforeAll
    static public void init() throws IOReactorException {

        //Start the Server first
        serverThread = new Thread(()->{
            try {
                System.out.println("Server started");
                String[] args = {};
                HttpServerHost.main(args);
                //HttpAsyncServer.main(args);
            } catch (Exception e){ e.printStackTrace(); return; }
        });
        serverThread.start();

        //Create Async Client
        httpproc = HttpProcessorBuilder.create()
                .add( new RequestContent())
                .add( new RequestTargetHost())
                .add( new RequestConnControl())
                .add( new RequestUserAgent("Test/1.1"))
                .add(new RequestExpectContinue(true))
                .build();
        HttpAsyncRequestExecutor protocolHandler = new HttpAsyncRequestExecutor();
        final IOEventDispatch ioEventDispatch = new DefaultHttpClientIODispatch(protocolHandler, ConnectionConfig.DEFAULT);
        ioReactor = new DefaultConnectingIOReactor();
        pool = new BasicNIOConnPool(ioReactor, ConnectionConfig.DEFAULT);
        pool.setDefaultMaxPerRoute(2);
        pool.setMaxTotal(2);
        clientThread = new Thread( () -> {
           try {
               ioReactor.execute(ioEventDispatch);
           }
           catch ( InterruptedIOException ex) {
                System.err.println("Interrupted");
           }
           catch (IOException e) {
               e.printStackTrace();
           }
           System.out.println("Shutdown the Thread");
        });
        clientThread.start();
    }


    @AfterAll
    static public void dispose() throws IOException {
        System.out.println("Shutting down I/O reactor");
        ioReactor.shutdown();
        System.out.println("Done");
    }



    public <T> void testRequest( final List<T> requests, final String action) throws InterruptedException {

        //Clear up previous record
        hasError = false;
        tokens.clear();
        statusCodes.clear();


        //Check if requests is not empty and are actually HttpRequests
        if (requests.isEmpty() )
            return;
        else if ( ! ((requests.get(0) instanceof BasicHttpRequest)
                || (requests.get(0) instanceof BasicHttpEntityEnclosingRequest))  ){
            hasError = true;
            System.out.println("your requests are not instance of HttpRequest");
            return;
        }

        long startTime = System.nanoTime();

        final HttpAsyncRequester requester = new HttpAsyncRequester(httpproc);
        final HttpHost target = new HttpHost("localhost", 8080, "http");
        final int requestTime = requests.size();
        final CountDownLatch latch = new CountDownLatch(requestTime);

        for ( int i = 0; i < requestTime; i++ ){
            final T request = requests.get(i);
            final HttpCoreContext coreContext = HttpCoreContext.create();

            BasicAsyncRequestProducer producer;
            if (request instanceof BasicHttpRequest )
                producer = new BasicAsyncRequestProducer(target, (BasicHttpRequest) request);
            else
                producer = new BasicAsyncRequestProducer(target, (BasicHttpEntityEnclosingRequest) request);

            var result = requester.execute(
                    producer,
                    new BasicAsyncResponseConsumer(),
                    pool,
                    coreContext,
                    new FutureCallback<HttpResponse>() {
                        @Override
                        public void completed(HttpResponse response) {
                            latch.countDown();
                            System.out.println( "request at " + requests.indexOf(request) + " is completed" +
                                    ", with status code of " + response.getStatusLine().getStatusCode() +
                                    ", operation " + action );

                            statusCodes.add(response.getStatusLine().getStatusCode());

                            if (action.equals(LOGIN)){
                                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ){
                                    try {
                                        String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                                        var arr = responseBody.split("\"");
                                        String token = arr[3];
                                        tokens.add(token);
                                    } catch (IOException e) {
                                        hasError = true;
                                    }
                                }
                            }
                        }

                        @Override
                        public void failed(Exception e){
                            latch.countDown();
                            System.out.println("Something is wrong");
                            hasError = true;
                        }


                        @Override
                        public void cancelled() {
                            latch.countDown();
                            System.out.println("Terminated");
                        }
                    }
            );
        }
        latch.await();
        long stopTime = System.nanoTime();
        double duration = (stopTime - startTime) / 1000000;
        System.out.println( "Performance is " + duration + " milliseconds\n");
    }



    @Test
    public void TestInit(){

    }



    @Test
    public void incorrectRequests() throws InterruptedException {
        List<BasicHttpRequest> requests = new ArrayList<>();
        requests.add(new BasicHttpRequest("GET", "/"));
        requests.add(new BasicHttpRequest("GET", "/"));
        requests.add(new BasicHttpRequest("GET", "/"));
        requests.add(new BasicHttpRequest("GET", "/"));

        testRequest(requests, "");
        assertNotNull(hasError);
        assertFalse(hasError);
    }




    /**
     * Login with the same legit username and password with 8 async request
     * @throws InterruptedException
     */
    @Test
    public void simpleloginRequests() throws  InterruptedException {
        final String jsonBody = "{\"Username\": \"" + "user00103" + "\",\"Password\": \"" + "pass00103" + "\"}";
        final HttpEntity requestEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);

        List<BasicHttpEntityEnclosingRequest> requests = new ArrayList<>();

        for (int i = 0; i < 8; i++ ){
            BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", "/BookManagementService/login", HttpVersion.HTTP_1_1);
            request.setEntity(requestEntity);
            requests.add(request);
        }

        testRequest(requests, LOGIN);
        assertNotNull(hasError);
        assertFalse(hasError);

        assertEquals(tokens.size(), 1);

        // Logout for that one user
        BasicHttpRequest logoutRequest = new BasicHttpRequest("GET",
                "/BookManagementService/logout?token=" + tokens.get(0) ,
                HttpVersion.HTTP_1_1);
        List<BasicHttpRequest> logoutRequests = new ArrayList<>();
        logoutRequests.add(logoutRequest);
        testRequest( logoutRequests , LOGOUT);

        assertNotNull(hasError);
        assertFalse(hasError);
    }


    @Test
    public void allUserSuccessLogin() throws InterruptedException {
        final int NumUsers = 10000;

        List<BasicHttpEntityEnclosingRequest> requests = new ArrayList<>();
        for (int i = 1; i < NumUsers + 1; i++ ){
            String i_str =  String.valueOf(i);
            String i_str_leadingZero = ("00000" + i_str ).substring(i_str.length());
            final String username = "user" + i_str_leadingZero;
            final String password = "pass" + i_str_leadingZero;

            final String jsonBody = "{\"Username\": \"" + username + "\",\"Password\": \"" + password + "\"}";
            final HttpEntity requestEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);

            BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST", "/BookManagementService/login", HttpVersion.HTTP_1_1);
            request.setEntity(requestEntity);
            requests.add(request);
        }

        testRequest(requests, LOGIN);
        assertNotNull(hasError);
        assertFalse(hasError);

        assertEquals(tokens.size(), NumUsers);

        // Logout
        List<BasicHttpRequest> logoutRequests = new ArrayList<>();
        for ( int j = 0; j < NumUsers; j++ ){
            BasicHttpRequest logoutRequest = new BasicHttpRequest("GET",
                    "/BookManagementService/logout?token=" + tokens.get(j) ,
                    HttpVersion.HTTP_1_1);
            logoutRequests.add(logoutRequest);
        }


        testRequest( logoutRequests , LOGOUT);
        int occurrences = Collections.frequency( statusCodes, HttpStatus.SC_OK );
        assertEquals(occurrences, NumUsers);
        assertNotNull(hasError);
        assertFalse(hasError);
    }

}

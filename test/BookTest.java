import com.mysql.fabric.Response;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultBHttpClientConnection;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.*;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import json.Book;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

enum SortBy{
    asc("asc"), desc("desc");
    public String s;
    SortBy(String s){this.s = s;}
}

enum Order{
    id("id"), author("author"), title("title");
    public String s;
    Order(String s){this.s = s;}
}


class BookTest {
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
        String username = "user0000" + Integer.toString(userNum) ;
        String password = "passwd0000" +  Integer.toString(userNum);
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



    /******* Books-Related-Operation *********/
    public HttpResponse AddBook(String title, String author, String publisher, int year) throws IOException, HttpException {
        if (!conn.isOpen()){
            conn.bind(new Socket(host.getHostName(), host.getPort()));
        }
        String jsonBody =
                "{\"Title\": \"" + title + "\"," +
                 "\"Author\": \"" + author + "\"," +
                 "\"Publisher\": \"" + publisher + "\"," +
                 "\"Year\": " + year +
                 "}";

        HttpEntity requestEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST",
                "/BookManagementService/books?token=" + userToken, HttpVersion.HTTP_1_1);
        request.setEntity(requestEntity);

        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        conn.close();
        return response;
    }


    //Return Response body
    public HttpResponse LookBook(@Nullable String title, @Nullable String author, @Nullable  Integer id, @Nullable SortBy sortBy,
                           @Nullable Order order, @Nullable Integer limit ) throws IOException, HttpException{
        if (!conn.isOpen()){
            conn.bind(new Socket(host.getHostName(), host.getPort()));
        }

        String url = "/BookManagementService/books?";
        if ( title != null )
            url = url + "title=" + title + "&";
        if ( author != null )
            url = url + "author=" + author + "&";
        if ( id != null )
            url = url + "id=" + id + "&";
        if ( limit != null )
            url = url + "limit=" + id + "&";
        if ( sortBy != null && order != null )
            url = url + "sortby=" + sortBy.s + "order=" + order.s + "&";
        url = url + "token=" + userToken;

        System.out.println("The lookUP Url is as follows: " + url );

        HttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        conn.close();
        return response;
    }


    //availability = true for loaning
    public HttpResponse loanOrReturnBook( int id, boolean availability ) throws IOException, HttpException{
        if (!conn.isOpen()){
            conn.bind(new Socket(host.getHostName(), host.getPort()));
        }
        String jsonBody = "{\"Available\":" + availability + "}";
        HttpEntity requestEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);

        String url = "/BookManagementService/books/" + id +  "?token=" + userToken ;
        BasicHttpEntityEnclosingRequest request = new  BasicHttpEntityEnclosingRequest("PUT", url, HttpVersion.HTTP_1_1);
        request.setEntity(requestEntity);

        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        conn.close();
        return response;
    }


    public HttpResponse deleteBook(int id) throws IOException, HttpException {
        if (!conn.isOpen()){
            conn.bind(new Socket(host.getHostName(), host.getPort()));
        }

        String url = "/BookManagementService/books/" + id +  "?token=" + userToken ;
        BasicHttpEntityEnclosingRequest request = new  BasicHttpEntityEnclosingRequest("DELETE", url, HttpVersion.HTTP_1_1);

        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        conn.close();
        return response;
    }


    @Test
    public void addAndDeleteABook() throws IOException, HttpException {
        HttpResponse response = AddBook("Onion adventure", "Oscar", "Oscar Ltd", 2020);
        assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        assertTrue(response.getHeaders("Location")[0].getValue().contains("/books/") );

        var arr = responseBody.split("/");
        int id = Integer.parseInt(arr[arr.length-1]);

        var response2 = deleteBook(id);
        assertEquals(HttpStatus.SC_OK,  response2.getStatusLine().getStatusCode());
    }


    @Test
    public void addRedundantBook() throws IOException, HttpException {
        HttpResponse response = AddBook("Tim Cook Recipe", "Tim", "AppleJuice Ltd", 2010);
        assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        assertTrue(response.getHeaders("Location")[0].getValue().contains("/books/") );

        var arr = responseBody.split("/");
        int id = Integer.parseInt(arr[arr.length-1]);

        HttpResponse response2 = AddBook("Tim Cook Recipe", "Tim", "AppleJuice Ltd", 2010);
        assertEquals(HttpStatus.SC_CONFLICT, response2.getStatusLine().getStatusCode());
        assertTrue(response2.getHeaders("Duplicate record")[0].getValue().contains("/books/") );

        var response3 = deleteBook(id);
        assertEquals(HttpStatus.SC_OK,  response3.getStatusLine().getStatusCode());
    }


    @Test
    public void lookForBooks() throws IOException, HttpException {

        /** Peform Adding of 10 books */
        int [] bookIDs = new int[10];
        for ( int i = 0; i < 10; i++ ){
            String title = "bookTitle" + i % 5 ;
            String author = "";
            if (i < 3 ){author = "Oscar"; }
            else if (i == 4 ){author = "JoJo"; }
            else author = "Mary";
            String publisher = (i < 4 ) ? "HKUST" : "Oscar Ltd";
            int year = 2000 + i;

            HttpResponse response = AddBook(title, author, publisher, year);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
            var arr = responseBody.split("/");
            int id = Integer.parseInt(arr[arr.length-1]);
            bookIDs[i] = id;
        }

        /**No Content*/
        HttpResponse noContentResponse = LookBook("dummy", "ShouldNOTEXist", null, null, null, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, noContentResponse.getStatusLine().getStatusCode());


        /** Perform Deletion */
        for ( int i = 0; i < 10; i++ ){
            HttpResponse response = deleteBook(bookIDs[i]);
            assertEquals(HttpStatus.SC_OK,  response.getStatusLine().getStatusCode());
        }

    }




}

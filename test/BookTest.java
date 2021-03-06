
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

enum Order{
    asc("asc"), desc("desc");
    public String s;
    Order(String s){this.s = s;}
}

enum SortBy{
    id("id"), author("author"), title("title");
    public String s;
    SortBy(String s){this.s = s;}
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
                    //HttpAsyncServer.main(args);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            });
            serverThread.start();
        } catch (Exception e ){ System.out.println("Something is wrong starting the server");   e.printStackTrace();  }
    }


    /******* User009 Login *********/
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
        String password = "pass0000" +  Integer.toString(userNum);
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

        if (response.getEntity() != null )
            responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        else responseBody = "";
        conn.close();
        return response;
    }




    public HttpResponse AddIncorrectBook(String jsonBody) throws IOException, HttpException  {
        if (!conn.isOpen()){
            conn.bind(new Socket(host.getHostName(), host.getPort()));
        }

        HttpEntity requestEntity = new StringEntity(jsonBody, ContentType.APPLICATION_JSON);
        BasicHttpEntityEnclosingRequest request = new BasicHttpEntityEnclosingRequest("POST",
                "/BookManagementService/books?token=" + userToken, HttpVersion.HTTP_1_1);
        request.setEntity(requestEntity);

        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        if (response.getEntity() != null )
            responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        else responseBody = "";
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
            url = url + "limit=" + limit + "&";
        if ( sortBy != null && order != null )
            url = url + "sortby=" + sortBy.s + "&" + "order=" + order.s + "&";
        url = url + "token=" + userToken;

        System.out.println("\nThe lookUP Url is as follows: " + url );

        HttpRequest request = new BasicHttpRequest("GET", url, HttpVersion.HTTP_1_1);
        httpexecutor.preProcess(request, httpproc, coreContext);
        HttpResponse response = httpexecutor.execute(request, conn, coreContext);
        httpexecutor.postProcess(response, httpproc, coreContext);

        if (response.getEntity() != null )
            responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        else responseBody = "";
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
        int id = Integer.parseInt(arr[arr.length-1].split("\\?")[0]);

        var response2 = deleteBook(id);
        assertEquals(HttpStatus.SC_OK,  response2.getStatusLine().getStatusCode());
    }

    @Test
    public  void deleteUnExistedBook() throws IOException, HttpException {
        var response = deleteBook(-10);
        assertEquals(404,  response.getStatusLine().getStatusCode());
    }


    @Test
    public void addRedundantBook() throws IOException, HttpException {
        HttpResponse response = AddBook("Tim Cook Recipe", "Tim", "AppleJuice Ltd", 2010);
        assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        assertTrue(response.getHeaders("Location")[0].getValue().contains("/books/") );

        var arr = responseBody.split("/");
        int id = Integer.parseInt(arr[arr.length-1].split("\\?")[0]);

        HttpResponse response2 = AddBook("Tim Cook Recipe", "Tim", "AppleJuice Ltd", 2010);
        assertEquals(HttpStatus.SC_CONFLICT, response2.getStatusLine().getStatusCode());
        assertTrue(response2.getHeaders("Duplicate record")[0].getValue().contains("/books/") );

        var response3 = deleteBook(id);
        assertEquals(HttpStatus.SC_OK,  response3.getStatusLine().getStatusCode());
    }


    @Test
    public void addIncorrectBook() throws IOException, HttpException {
        HttpResponse response = AddIncorrectBook("{}");
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());

        HttpResponse response1 = AddIncorrectBook("{....Hello}");
        assertEquals(HttpStatus.SC_BAD_REQUEST, response1.getStatusLine().getStatusCode());

        HttpResponse response2 =  AddIncorrectBook("{\"Title\": \"" + "HELLOWORLD" + "\"");
        assertEquals(HttpStatus.SC_BAD_REQUEST, response2.getStatusLine().getStatusCode());
    }



    @Test
    public void lookForBooks() throws IOException, HttpException {

        /** Peform Adding of 10 books */
        int [] bookIDs = new int[10];
        for ( int i = 0; i < 10; i++ ){
            String title = "bookTitle" + i;
            String author = "";
            if (i < 3 ){author = "Oscar"; }
            else if (i == 4 ){author = "JoJo"; }
            else author = "Mary";
            String publisher = (i < 4 ) ? "HKUST" : "Oscar Ltd";
            int year = 2000 + i;

            HttpResponse response = AddBook(title, author, publisher, year);
            assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
            var arr = responseBody.split("/");
            int id = Integer.parseInt(arr[arr.length-1].split("\\?")[0]);
            bookIDs[i] = id;
        }

        /** Find Book*/
        var bookResponse = LookBook(null, "Oscar", null, null, null, null );
        assertEquals(HttpStatus.SC_OK, bookResponse.getStatusLine().getStatusCode());
        assertTrue(responseBody.contains("\"FoundBooks\":3"));

        /** Find Book with no statement */
        bookResponse = LookBook(null, null, null, null, null, null );
        assertEquals(HttpStatus.SC_OK, bookResponse.getStatusLine().getStatusCode());
        assertTrue(responseBody.contains("FoundBooks"));


        /** Find Book with id order*/
        bookResponse = LookBook("bookTitle", "Oscar", null, SortBy.id, Order.desc, null );
        assertEquals(HttpStatus.SC_OK, bookResponse.getStatusLine().getStatusCode() );


        /** Find Book with lower limit*/
        bookResponse = LookBook("bookTitle", "Oscar", null, null, null, 2 );
        assertEquals(HttpStatus.SC_OK, bookResponse.getStatusLine().getStatusCode());
        assertTrue(responseBody.contains("\"FoundBooks\":2"));


        /** Find Book with upper limit*/
        bookResponse = LookBook("bookTitle", "Oscar", null, null, null, 7 );
        assertEquals(HttpStatus.SC_OK, bookResponse.getStatusLine().getStatusCode());
        assertTrue(responseBody.contains("\"FoundBooks\":3"));


        /**No Content*/
        var noContentResponse = LookBook("dummy", "ShouldNOTEXist", null, null, null, null);
        assertEquals(HttpStatus.SC_NO_CONTENT, noContentResponse.getStatusLine().getStatusCode());


        /** Perform Deletion */
        for ( int i = 0; i < 10; i++ ){
            HttpResponse response = deleteBook(bookIDs[i]);
            assertEquals(HttpStatus.SC_OK,  response.getStatusLine().getStatusCode());
        }

    }


    @Test
    public void loanAndReturnBook() throws IOException, HttpException{
        /** Add a Book */
        HttpResponse response = AddBook("Onion adventure", "OscarNg", "Oscar Ltd", 2020);
        assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
        assertTrue(response.getHeaders("Location")[0].getValue().contains("/books/") );
        var arr = responseBody.split("/");
        int id = Integer.parseInt(arr[arr.length-1].split("\\?")[0] );

        /** Loan a Book */
        response = loanOrReturnBook(id, false);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode() );

        /** Loan the same book again */
        response = loanOrReturnBook(id, false);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode() );

        /** Return a Book */
        response = loanOrReturnBook(id, true);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode() );

        /** Return a Book that is loaned */
        response = loanOrReturnBook(id, true);
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode() );

        /** Delete the Book */
        response = deleteBook(id);
        assertEquals(HttpStatus.SC_OK,  response.getStatusLine().getStatusCode());
    }


    @Test
    public void loanAndReturnUnexistedBook() throws IOException, HttpException {
        /** Loan a book that doesn't exist */
        var response = loanOrReturnBook(-3, false);
        assertEquals(404, response.getStatusLine().getStatusCode() );

        /** Return a Book that doesn't exist */
        response = loanOrReturnBook(-3, true);
        assertEquals(404, response.getStatusLine().getStatusCode() );
    }



}

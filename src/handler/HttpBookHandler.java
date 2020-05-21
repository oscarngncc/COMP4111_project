package handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.GeneralHelpers;
import helper.HttpHelpers;
import helper.SqlHelpers;
import json.Availability;
import json.Book;
import json.BookList;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncExchange;
import org.apache.http.nio.protocol.HttpAsyncRequestConsumer;
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class HttpBookHandler implements HttpAsyncRequestHandler {
    @Override
    public HttpAsyncRequestConsumer processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(Object o, HttpAsyncExchange httpAsyncExchange, HttpContext httpContext) throws HttpException, IOException {
        HttpRequest request = httpAsyncExchange.getRequest();
        HttpResponse response = httpAsyncExchange.getResponse();
        handleInternal(request, response, httpContext);
        httpAsyncExchange.submitResponse();
    }

    public void handleInternal(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);

        if (!method.equals("GET") && !method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE")) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
        //Set default response
        response.setStatusCode(HttpStatus.SC_OK);

        //Try to get token from url
        Map<String, String> params;
        String token;
        try{
            params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
            token = params.get("TOKEN");
            //Check whether the token is valid
            boolean token_found = SqlHelpers.IsTokenFound(token);
            if(!token_found) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
        }catch (Exception e){
            // Out of scope invalid input handling
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }

        //Config JSON Mapper
        ObjectMapper mapper = HttpHelpers.setUpMapper();


        //Handle Get Method
        if(method.equals("GET")){
            try {
                //Get book info from string params in url
                Book book = GeneralHelpers.GetBookFromParams(params);
                String sortBy = "";
                boolean asc = true;
                int limit = 0;

                //Check if user input limit
                if (params.containsKey("LIMIT")){
                    if ( Integer.parseInt(params.get("LIMIT")) > 0 )
                        limit = Integer.parseInt(params.get("LIMIT"));
                }
                //Check if user input sortby and order
                if(params.containsKey("SORTBY") && params.containsKey("ORDER") ) {
                    sortBy = params.get("SORTBY").toUpperCase();
                    sortBy = (sortBy.equals("ID")  ||
                            sortBy.equals("AUTHOR") ||
                            sortBy.equals("TITLE") ||
                            sortBy.equals("PUBLISHER") ||
                            sortBy.equals("YEAR") ||
                            sortBy.equals("AVAILABLE")
                    ) ? sortBy : "";
                    asc = ( params.get("ORDER").equals("DESC") ) ? false : true;
                }

                //Look up books in DB and store results in BookList
                BookList bookList = SqlHelpers.LookUpBook(book, limit, sortBy, asc);

                //If no book is found, return NO CONTENT
                if (bookList.getFoundBooks() == 0 ){
                    response.setStatusCode(HttpStatus.SC_NO_CONTENT);
                    return;
                }

                //If books are found, convert result to JSON message and return in the response body
                String jsonString ="";
                try {
                    jsonString = mapper.writeValueAsString(bookList);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                StringEntity entity = new StringEntity(
                        jsonString,
                        ContentType.create("application/json", Consts.UTF_8));
                response.setEntity(entity);
                return;
            }catch(Exception e){
                // Out of scope invalid input handling
                response.setStatusCode(HttpStatus.SC_NO_CONTENT);
                return;
            }
        }

        //Handle DELETE Method
        if(method.equals("DELETE")){
            try{
                // Get Book id from url
                int id = GeneralHelpers.GetBookIdFromUrl(request.getRequestLine().getUri());
                // Check the id is valid and delete it if valid
                if(!SqlHelpers.DeleteBook(id)){
                    //Change the httpstatus from default 200
                    response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
                }
                return;
            }catch(Exception e) {
                // Out of scope invalid input handling
                response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
            }
        }


        // Check if JSON body exist
        String retSrc = null;
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            retSrc = EntityUtils.toString(entity);
        }else{
            response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");

        }

        //  Handle POST Method
        if(method.equals("POST")){
            try {
                //Map the JSON to book
                if (retSrc.isEmpty() || retSrc == null) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
                Book book = mapper.readValue(retSrc, Book.class);

                //Prevent error caused by book and year
                if (book.getTitle() == "" ){
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                } else if (book.getYear().length() > 4) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                StringEntity entity = null;
                //Check whether there is an identical book
                int id = SqlHelpers.InsertBook(book);
                if (id > 0) {
                    //Insert the book to DB as no identical book
                    response.setStatusCode(HttpStatus.SC_CREATED);
                    response.setHeader("Location", "/books/" + id);
                    String entityText = "http://localhost:8080/BookManagementService/books/" + id + "?token=" + token;
                    entity = new StringEntity(entityText, ContentType.TEXT_PLAIN);
                } else {
                    //Return the id of the identical book
                    id = SqlHelpers.FindIdenticalBook(book);
                    response.setStatusCode(HttpStatus.SC_CONFLICT);
                    response.setHeader("Duplicate record", "/books/" + id);
                    entity = new StringEntity("Duplicate record: /books/" + id, ContentType.TEXT_PLAIN);
                }
                //set response body and return
                response.setEntity(entity);
                return;
            } catch (Exception e){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            }
        }

        //Handle PUT Method
        if(method.equals("PUT")){
            try {
                //Get the book id from url
                int id = GeneralHelpers.GetBookIdFromUrl(request.getRequestLine().getUri());

                int status = 0;
                //Get availability from the JSON message to indicate whether it is a loan action or a return action
                if(retSrc.isEmpty() || retSrc == null){
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
                Availability availability = mapper.readValue(retSrc, Availability.class);
                if (!availability.isAvailable()) {
                    //Handle loaning
                    status = SqlHelpers.LoanBook(id);
                }
                if (availability.isAvailable()) {
                    //Handle returning
                    status = SqlHelpers.ReturnBook(id);
                }
                //Check the return status from DB
                switch (status) {
                    case 10:
                        //Book not found
                        response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
                        break;
                    case 15:
                        //Book is already loaned or returned
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        break;
                    case 20:
                        //Book loaned or returned successfully
                        response.setStatusCode(HttpStatus.SC_OK);
                        break;
                }
                return;
            }
            catch (JsonProcessingException e) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            }
            catch (Exception e){
                // Out of scope invalid input handling
                response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
            }
        }
    }
}

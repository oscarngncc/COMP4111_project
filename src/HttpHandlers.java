import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import json.*;
/**
 * This the class of Http Handlers
 *
 */
public class HttpHandlers {
    /**
     * This the class of the HttpHandler of Login function
     *
     */
    static class HttpLoginHandler implements HttpRequestHandler {
        /**
         * This the constructor of the HttpHandler
         *
         */
        public HttpLoginHandler() {
            super();
        }
        /**
         * This the handle method of the HttpHandler
         * @param request HttpRequest
         * @param response HttpResponse
         * @param context HttpContext
         */
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            // Check whether the method is valid
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if (!method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }


            try {
                // Try to get the JSON message from body, if not return 400 bad request
                String retSrc;
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    retSrc = EntityUtils.toString(entity);
                } else {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                // Map the JSON message to User class
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,true);
                //JSON file to Java object
                if(retSrc.isEmpty() || retSrc == null){
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
                User user = mapper.readValue(retSrc, User.class);

                // Set Default response
                response.setStatusCode(HttpStatus.SC_OK);

                // Check the user info is found in the DB
                boolean user_found = SqlHelpers.IsUserFound(user.getUsername(), user.getPassword());
                if (!user_found) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                // Check whether there is already a token for this user
                boolean token_found = SqlHelpers.IsUserTokenFound(user.getUsername().substring(4));
                if (token_found) {
                    response.setStatusCode(HttpStatus.SC_CONFLICT);
                    return;
                }

                // Generate Token
                String token = GeneralHelpers.GenerateToken(user.getUsername().substring(4));

                // Insert Token to DB
                SqlHelpers.InsertToken(token);

                // Set response body
                StringEntity entity = new StringEntity(
                        "{\"Token\": \"" + token + "\"}",
                        ContentType.create("application/json", Consts.UTF_8));
                response.setEntity(entity);
            }catch(Exception e){
                // Out of scope invalid input handling
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
        }
    }
    /**
     * This the class of a HttpHandler of Logout function
     *
     */
    static class HttpLogoutHandler implements HttpRequestHandler {
        /**
         * This the constructor of the HttpHandler
         *
         */
        public HttpLogoutHandler() {
            super();
        }
        /**
         * This the handle method of the HttpHandler
         * @param request HttpRequest
         * @param response HttpResponse
         * @param context HttpContext
         */
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            // Check whether the method is valid
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if (!method.equals("GET")) {
                //return 400 Bad Request for invalid method
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            try {
                //set default response
                response.setStatusCode(HttpStatus.SC_OK);

                //Get the token from url
                Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
                String token = params.get("TOKEN");

                //Check whether the token is valid or not
                boolean token_found = SqlHelpers.IsTokenFound(token);

                //If token is valid, delete token in DB
                if (token_found) {
                    SqlHelpers.DeleteToken(token);
                } else {
                    //Otherwise, return 400 Bad request
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                }
            }catch (Exception e){
                // Out of scope invalid input handling
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
        }
    }
    /**
     * This the class of a HttpHandler of Book Management Functions
     *
     */
    static class HttpBookHandler implements HttpRequestHandler {
        /**
         * This the constructor of the HttpHandler
         *
         */
        public HttpBookHandler() {
            super();
        }
        /**
         * This the handle method of the HttpHandler
         * @param request HttpRequest
         * @param response HttpResponse
         * @param context HttpContext
         */
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            //Check whether the method is valid or not
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
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,true);

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
                //Map the JSON to book
                if(retSrc.isEmpty() || retSrc == null){
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
                Book book = mapper.readValue(retSrc, Book.class);
                StringEntity entity = null;
                //Check whether there is an identical book
                int id = SqlHelpers.FindIdenticalBook(book);
                if(id == 0){
                    //Insert the book to DB as no identical book
                    id = SqlHelpers.InsertBook(book);
                    response.setStatusCode(HttpStatus.SC_CREATED);
                    response.setHeader("Location", "/books/" + id );
                    String entityText = "http://localhost:8080/BookManagementService/books/" + id + "?token=" + token;
                    entity = new StringEntity(entityText, ContentType.TEXT_PLAIN);
                }else{
                    //Return the id of the identical book
                    response.setStatusCode(HttpStatus.SC_CONFLICT);
                    response.setHeader("Duplicate record", "/books/" + id );
                    entity = new StringEntity("Duplicate record: /books/"+ id, ContentType.TEXT_PLAIN);
                }
                //set response body and return
                response.setEntity(entity);
                return;
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
                }catch (Exception e){
                    // Out of scope invalid input handling
                    response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
                }
            }
        }
    }

    /**
     * This the class of a HttpHandler of Transaction Behaviors
     *
     */
    static class HttpTransactionHandler implements HttpRequestHandler {
        /**
         * This the constructor of the HttpHandler
         *
         */
        public HttpTransactionHandler() {
            super();
        }
        /**
         * This the handle method of the HttpHandler
         * @param request HttpRequest
         * @param response HttpResponse
         * @param context HttpContext
         */
        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            //Check whether the method is valid or not
            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if (!method.equals("POST") && !method.equals("PUT")) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
            try {
                //Set default response status
                response.setStatusCode(HttpStatus.SC_OK);

                //Get params from url and validate the token
                Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
                String token = params.get("TOKEN");
                boolean token_found = SqlHelpers.IsTokenFound(token);
                if (!token_found) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                //Check if the body is empty or not
                boolean isEmptyBody = false;
                String retSrc = null;
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    retSrc = EntityUtils.toString(entity);
                } else {
                    isEmptyBody = true;
                }
                if (isEmptyBody || retSrc.isEmpty()) {
                    if (method.equals("POST")) {
                        //If nothing is found in request body and it is a POST method, start transaction
                        int transactionId = SqlHelpers.InsertTransaction(token);
                        if (transactionId <= 0) {
                            //transaction is already started
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                            return;
                        }else{
                            //Return the assigned transaction id
                            StringEntity entity = new StringEntity(
                                    "{\"Transaction\": " + transactionId + "}",
                                    ContentType.create("application/json", Consts.UTF_8));
                            response.setEntity(entity);
                        }
                    } else {
                        //Invalid request
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    }
                    return;
                }

                //Map the JSON message to a transaction object
                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,true);
                Transaction transaction = mapper.readValue(retSrc, Transaction.class);

                //Check whether the transaction id is valid or not
                if (!SqlHelpers.IsTransactionIdFound(transaction.getTransactionId(), token)) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                //Handle POST Method
                if (method.equals("POST")) {
                    //Check whether it is a commit operation
                    if (transaction.getOperation().toUpperCase().equals("COMMIT")) {
                        //return 400 if it is invalid otherwise the default 200
                        if (!SqlHelpers.CommitTransaction(transaction)) {
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        }
                        return;
                    }
                    //Check whether it is a cancel operation
                    if (transaction.getOperation().toUpperCase().equals("CANCEL")) {
                        //return 400 if it is invalid otherwise the default 200
                        if (!SqlHelpers.CancelTransaction(transaction)) {
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        }
                        return;
                    }
                    //Invalid operation
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                //Handle PUT Method
                if (method.equals("PUT")) {
                    //Perform action and check it is ok
                    if (!SqlHelpers.UpdateTransaction(transaction)) {
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    }
                    return;
                }
            }catch(Exception e){
                // Out of scope invalid input handling
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
        }
    }
}

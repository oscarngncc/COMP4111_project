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

public class HttpHandlers {

    static class HttpLoginHandler implements HttpRequestHandler {

        public HttpLoginHandler() {
            super();
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if (!method.equals("POST")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }
            String retSrc;
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                retSrc = EntityUtils.toString(entity);
            }else{
                throw new MethodNotSupportedException("JSON not found");
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            //JSON file to Java object
            User user = mapper.readValue(retSrc, User.class);

            response.setStatusCode(HttpStatus.SC_OK);

            boolean user_found = SqlHelpers.IsUserFound(user.getUsername(), user.getPassword());
            if(!user_found){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            boolean token_found = SqlHelpers.IsUserTokenFound(user.getUsername().substring(4));
            if(token_found){
                response.setStatusCode(HttpStatus.SC_CONFLICT);
                return;
            }

            String token = GeneralHelpers.GenerateToken(user.getUsername().substring(4));

            SqlHelpers.InsertToken(token);
            StringEntity entity = new StringEntity(
                    "{\"Token\": \"" + token + "\"}",
                    ContentType.create("application/json", Consts.UTF_8));
            response.setEntity(entity);
        }
    }

    static class HttpLogoutHandler implements HttpRequestHandler {

        public HttpLogoutHandler() {
            super();
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if (!method.equals("GET")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            response.setStatusCode(HttpStatus.SC_OK);

            Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
            String token = params.get("token");

            boolean token_found = SqlHelpers.IsTokenFound(token);

            if(token_found) {
                SqlHelpers.DeleteToken(token);
            }else{
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            }
        }
    }

    static class HttpBookHandler implements HttpRequestHandler {

        public HttpBookHandler() {
            super();
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);

            if (!method.equals("GET") && !method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            response.setStatusCode(HttpStatus.SC_OK);

            Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
            String token = params.get("token");
            boolean token_found = SqlHelpers.IsTokenFound(token);
            if(!token_found) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            if(method.equals("GET")){
                Book book = GeneralHelpers.GetBookFromParams(params);
                String sortBy = "";
                boolean asc = true;
                int limit = 0;
                if(params.containsKey("sortBy"))
                    sortBy = params.get("sortBy");
                if(params.containsKey("asc"))
                    if(params.get("asc").equals("false"))
                        asc = false;
                BookList bookList = SqlHelpers.LookUpBook(book, limit, sortBy, asc);
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
            }

            String retSrc;
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                retSrc = EntityUtils.toString(entity);
            }else{
                throw new MethodNotSupportedException("JSON not found");
            }
            if(method.equals("POST")){
                Book book = mapper.readValue(retSrc, Book.class);
                StringEntity entity = null;
                int id = SqlHelpers.FindIdenticalBook(book);
                if(id == 0){
                    id = SqlHelpers.InsertBook(book);
                    response.setStatusCode(HttpStatus.SC_CREATED);
                    entity = new StringEntity(
                            "<html><body></body>HTTP/1.1 201 Created\n" +
                                    "Location: /books/"+ id +"</html>",
                            ContentType.create("text/html", "UTF-8"));
                }else{
                    response.setStatusCode(HttpStatus.SC_CONFLICT);
                    entity = new StringEntity(
                            "<html><body></body>HTTP/1.1 409 Conflict\n" +
                                    "Duplicate record: /books/"+ id +"</html>",
                            ContentType.create("text/html", "UTF-8"));
                }
                response.setEntity(entity);
                return;
            }

            int id = GeneralHelpers.GetBookIdFromUrl(request.getRequestLine().getUri());

            if(method.equals("PUT")){
                int status = 0;
                Availability availability = mapper.readValue(retSrc,Availability.class);
                if (!availability.isAvailable()){
                    status = SqlHelpers.LoanBook(id);
                }
                if (availability.isAvailable()){
                    status = SqlHelpers.ReturnBook(id);
                }
                switch (status){
                    case 10 : response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                    case 15 : response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                }
                return;
            }

            if(method.equals("DELETE")){
                if(!SqlHelpers.DeleteBook(id)){
                    response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                }
                return;
            }
        }
    }


    static class HttpTransactionHandler implements HttpRequestHandler {

        public HttpTransactionHandler() {
            super();
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if (!method.equals("POST") && !method.equals("PuT")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }

            response.setStatusCode(HttpStatus.SC_OK);

            Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
            String token = params.get("token");
            boolean token_found = SqlHelpers.IsTokenFound(token);
            if(!token_found) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            String retSrc = null;
            if (request instanceof HttpEntityEnclosingRequest) {
                HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                retSrc = EntityUtils.toString(entity);
            }else{
                if(method.equals("POST")){
                    // Generate new tranaction id
                    String transactionId = GeneralHelpers.GenerateTransactionId(token.substring(0,5));
                    SqlHelpers.InsertTransaction(transactionId);
                    StringEntity entity = new StringEntity(
                            "{\"Transaction\": \"" + transactionId + "\"}",
                            ContentType.create("application/json", Consts.UTF_8));
                    response.setEntity(entity);
                }else {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                }
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Transaction transaction = mapper.readValue(retSrc, Transaction.class);

            boolean transaction_found = SqlHelpers.IsTransactionIdFound(transaction.getTransactionId());
            if(!transaction_found){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            if(method.equals("POST")){
                if(transaction.getOperation().equals("commit")){
                    if (!SqlHelpers.CommitTransaction(transaction.getTransactionId())){
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    }
                    return;
                }
                if(transaction.getOperation().equals("cancel")){
                    if (!SqlHelpers.CancelTransaction(transaction.getTransactionId())){
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    }
                    return;
                }
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            if(method.equals("PUT")){
                if(!SqlHelpers.UpdateTransaction(transaction)){
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                }
                return;
            }
        }
    }
}

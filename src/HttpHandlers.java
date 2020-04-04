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

                if (params.containsKey("limit")){
                    if ( Integer.parseInt(params.get("limit")) > 0 )
                        limit = Integer.parseInt(params.get("limit"));
                }

                if(params.containsKey("sortby") && params.containsKey("order") ) {
                    System.out.println("YESSSSSSSSSSSSSSSSSSSSSS " + params.get("sortby") );
                    sortBy = params.get("sortby");
                    sortBy = (sortBy.equals("id")  || sortBy.equals("author") || sortBy.equals("title") ) ? sortBy : "";
                    asc = ( params.get("order").equals("desc") ) ? false : true;
                }

                BookList bookList = SqlHelpers.LookUpBook(book, limit, sortBy, asc);

                if (bookList.getFoundBooks() == 0 ){
                    response.setStatusCode(HttpStatus.SC_NO_CONTENT);
                    return;
                }

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


            if(method.equals("DELETE")){
                int id = GeneralHelpers.GetBookIdFromUrl(request.getRequestLine().getUri());
                if(!SqlHelpers.DeleteBook(id)){
                    response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No Book Record");
                }
                return;
            }


            /** Check if JSON body exist */
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
                    response.setHeader("Location", "/books/" + id );
                    entity = new StringEntity("Location: /books/" + id, ContentType.TEXT_PLAIN);
                }else{
                    response.setStatusCode(HttpStatus.SC_CONFLICT);
                    response.setHeader("Duplicate record", "/books/" + id );
                    entity = new StringEntity("Duplicate record: /books/"+ id, ContentType.TEXT_PLAIN);
                }
                response.setEntity(entity);
                return;
            }


            if(method.equals("PUT")){
                int id = GeneralHelpers.GetBookIdFromUrl(request.getRequestLine().getUri());
                int status = 0;
                Availability availability = mapper.readValue(retSrc,Availability.class);
                if (!availability.isAvailable()){
                    status = SqlHelpers.LoanBook(id);
                }
                if (availability.isAvailable()){
                    status = SqlHelpers.ReturnBook(id);
                }
                switch (status){
                    case 10 : response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No Book Record"); break;
                    case 15 : response.setStatusCode(HttpStatus.SC_BAD_REQUEST); break;
                    case 20: response.setStatusCode(HttpStatus.SC_OK); break;
                }
                return;
            }
        }
    }


    /** Need fix */
    static class HttpTransactionHandler implements HttpRequestHandler {

        public HttpTransactionHandler() {
            super();
        }

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {

            String method = request.getRequestLine().getMethod().toUpperCase(Locale.ROOT);
            if (!method.equals("POST") && !method.equals("PUT")) {
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
            }
            else if (! method.equals("POST")) {
                throw new MethodNotSupportedException("JSON not found");
            }


            if(method.equals("POST") && (retSrc == null || retSrc.equals("") ) ){
                // Generate new tranaction id
                String transactionId = GeneralHelpers.GenerateTransactionId(token.substring(0,5));
                SqlHelpers.InsertTransaction(transactionId);
                StringEntity entity = new StringEntity(
                        "{\"Transaction\":" + transactionId + "}",
                        ContentType.create("application/json", Consts.UTF_8));
                response.setEntity(entity);
                return;
            }


            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Transaction transaction = mapper.readValue(retSrc, Transaction.class);

            //boolean transaction_found = SqlHelpers.IsTransactionIdFound(transaction.getTransactionId());
            boolean transaction_found = false;
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

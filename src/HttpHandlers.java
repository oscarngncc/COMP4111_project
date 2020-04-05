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
            try {
                String retSrc;
                if (request instanceof HttpEntityEnclosingRequest) {
                    HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
                    retSrc = EntityUtils.toString(entity);
                } else {
                    throw new MethodNotSupportedException("JSON not found");
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,true);
                //JSON file to Java object
                User user = mapper.readValue(retSrc, User.class);


                response.setStatusCode(HttpStatus.SC_OK);

                boolean user_found = SqlHelpers.IsUserFound(user.getUsername(), user.getPassword());
                if (!user_found) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                boolean token_found = SqlHelpers.IsUserTokenFound(user.getUsername().substring(4));
                if (token_found) {
                    response.setStatusCode(HttpStatus.SC_CONFLICT);
                    return;
                }

                String token = GeneralHelpers.GenerateToken(user.getUsername().substring(4));

                SqlHelpers.InsertToken(token);
                StringEntity entity = new StringEntity(
                        "{\"Token\": \"" + token + "\"}",
                        ContentType.create("application/json", Consts.UTF_8));
                response.setEntity(entity);
            }catch(Exception e){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
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
            try {
                response.setStatusCode(HttpStatus.SC_OK);

                Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
                String token = params.get("token");

                boolean token_found = SqlHelpers.IsTokenFound(token);

                if (token_found) {
                    SqlHelpers.DeleteToken(token);
                } else {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                }
            }catch (Exception e){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
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
            Map<String, String> params;
            String token;
            try{
                params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
                token = params.get("token");
                boolean token_found = SqlHelpers.IsTokenFound(token);
                if(!token_found) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
            }catch (Exception e){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,true);

            if(method.equals("GET")){
                try {
                    Book book = GeneralHelpers.GetBookFromParams(params);
                    String sortBy = "";
                    boolean asc = true;
                    int limit = 0;

                    if (params.containsKey("limit")){
                        if ( Integer.parseInt(params.get("limit")) > 0 )
                            limit = Integer.parseInt(params.get("limit"));
                    }

                    if(params.containsKey("sortby") && params.containsKey("order") ) {
                        sortBy = params.get("sortby").toUpperCase();
                        sortBy = (sortBy.equals("ID")  || sortBy.equals("AUTHOR") || sortBy.equals("TITLE") ) ? sortBy : "";
                        asc = ( params.get("order").toUpperCase().equals("DESC") ) ? false : true;
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
                }catch(Exception e){
                    response.setStatusCode(HttpStatus.SC_NO_CONTENT);
                    return;
                }
            }


            if(method.equals("DELETE")){
                try{
                    int id = GeneralHelpers.GetBookIdFromUrl(request.getRequestLine().getUri());
                    if(!SqlHelpers.DeleteBook(id)){
                        response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
                    }
                    return;
                }catch(Exception e) {
                    response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
                }
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
                    String entityText = "http://localhost:8080/BookManagementService/books/" + id + "?token=" + token;
                    entity = new StringEntity(entityText, ContentType.TEXT_PLAIN);
                }else{
                    response.setStatusCode(HttpStatus.SC_CONFLICT);
                    response.setHeader("Duplicate record", "/books/" + id );
                    entity = new StringEntity("Duplicate record: /books/"+ id, ContentType.TEXT_PLAIN);
                }
                response.setEntity(entity);
                return;
            }


            if(method.equals("PUT")){
                try {
                    int id = GeneralHelpers.GetBookIdFromUrl(request.getRequestLine().getUri());
                    int status = 0;
                    Availability availability = mapper.readValue(retSrc, Availability.class);
                    if (!availability.isAvailable()) {
                        status = SqlHelpers.LoanBook(id, false);
                    }
                    if (availability.isAvailable()) {
                        status = SqlHelpers.ReturnBook(id, false);
                    }
                    switch (status) {
                        case 10:
                            response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
                            break;
                        case 15:
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                            break;
                        case 20:
                            response.setStatusCode(HttpStatus.SC_OK);
                            break;
                    }
                    return;
                }catch (Exception e){
                    response.setStatusLine(new ProtocolVersion("HTTP", 1, 1), 404, "No book record");
                }
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
            if (!method.equals("POST") && !method.equals("PUT")) {
                throw new MethodNotSupportedException(method + " method not supported");
            }
            try {
                response.setStatusCode(HttpStatus.SC_OK);

                Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
                String token = params.get("token");
                boolean token_found = SqlHelpers.IsTokenFound(token);
                if (!token_found) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }
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
                        // Generate new tranaction id
                        int transactionId = SqlHelpers.InsertTransaction(token);
                        if (transactionId == 0) {
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                            return;
                        }
                        if (transactionId > 0) {
                            StringEntity entity = new StringEntity(
                                    "{\"Transaction\": \"" + transactionId + "\"}",
                                    ContentType.create("application/json", Consts.UTF_8));
                            response.setEntity(entity);
                        } else {
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        }
                    } else {
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    }
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,true);
                Transaction transaction = mapper.readValue(retSrc, Transaction.class);

                int transactionIdStatus = SqlHelpers.IsTransactionIdFound(transaction.getTransactionId(), token);
                if (transactionIdStatus != 20) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                if (method.equals("POST")) {
                    if (transaction.getOperation().toUpperCase().equals("COMMIT")) {
                        if (!SqlHelpers.CommitTransaction()) {
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        }
                        return;
                    }
                    if (transaction.getOperation().toUpperCase().equals("CANCEL")) {
                        if (!SqlHelpers.CancelTransaction()) {
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        }
                        return;
                    }
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    return;
                }

                if (method.equals("PUT")) {
                    if (!SqlHelpers.UpdateTransaction(transaction)) {
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    }
                    return;
                }
            }catch(Exception e){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }
        }
    }
}

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

import java.awt.desktop.SystemSleepEvent;
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

            User user = new User(
                    request.getHeaders("Username")[0].getValue(),
                    request.getHeaders("Password")[0].getValue()
            );


            //Check User Exist
            boolean user_found = SqlHelpers.IsUserFound(user.getUsername(), user.getPassword());
            if(!user_found){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            //Check Token Found (Double Login)
            boolean token_found = SqlHelpers.IsUserTokenFound(user.getUsername().substring(4));
            if(token_found){
                response.setStatusCode(HttpStatus.SC_CONFLICT);
                return;
            }

            //Success
            String token = GeneralHelpers.GenerateToken(user.getUsername().substring(4));
            SqlHelpers.InsertToken(token);
            StringEntity entity = new StringEntity(
                    "{\"Token\": \"" + token + "\"}",
                    ContentType.create("application/json", Consts.UTF_8));
            response.setStatusCode(HttpStatus.SC_OK);
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


            Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
            String token = params.get("token");


            if(SqlHelpers.IsTokenFound(token)) {
                SqlHelpers.DeleteToken(token);
                response.setStatusCode(HttpStatus.SC_OK);
                return;
            }else{
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

            Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
            String token = params.get("token");
            boolean token_found = SqlHelpers.IsTokenFound(token);
            if(!token_found) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }



            if(method.equals("GET")){

            }

            if(method.equals("POST")){

            }

            if(method.equals("PUT")){

            }

            if(method.equals("DELETE")){

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
                    return;
                }
                if(transaction.getOperation().equals("cancel")){
                    return;
                }
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            if(method.equals("PUT")){

                return;
            }
        }
    }
}

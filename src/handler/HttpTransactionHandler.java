package handler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import helper.GeneralHelpers;
import helper.HttpHelpers;
import helper.SqlHelpers;
import json.Transaction;
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

public class HttpTransactionHandler implements HttpAsyncRequestHandler {

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
                    } else {
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
            mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
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
        } catch (Exception e) {
            // Out of scope invalid input handling
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
    }
}
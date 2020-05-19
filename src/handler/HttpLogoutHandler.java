package handler;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import json.User;
import helper.*;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public class HttpLogoutHandler implements HttpAsyncRequestHandler {
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
        if (!method.equals("GET")) {
            //return 400 Bad Request for invalid method
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
        }
        try {
            //Get the token from url
            Map<String, String> params = GeneralHelpers.GetParamsMap(request.getRequestLine().getUri());
            String token = params.get("TOKEN");

            //Check whether the token is valid or not
            boolean result = SqlHelpers.DeleteToken(token);

            //If token is valid, delete token in DB
            if (result) {
                response.setStatusCode(HttpStatus.SC_OK);
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

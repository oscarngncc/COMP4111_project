package handler;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import json.User;
import helper.*;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Locale;

public class HttpLoginHandler implements HttpAsyncRequestHandler {
    /**
     * This the constructor of the HttpHandler
     *
     */
    public HttpLoginHandler() {
        super();
    }

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

    public void handleInternal(HttpRequest request, HttpResponse response, HttpContext httpContext)throws HttpException, IOException {
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
            if(retSrc.isEmpty() || retSrc == null){
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            // Map the JSON message to User class
            ObjectMapper mapper = HttpHelpers.setUpMapper();
            User user = mapper.readValue(retSrc, User.class);

            // Set Default response
            response.setStatusCode(HttpStatus.SC_OK);

            // Generate Token
            String token = GeneralHelpers.GenerateToken(user.getUsername().substring(4));

            boolean user_found = SqlHelpers.IsUserFound(user.getUsername(), user.getPassword());
            if (!user_found) {
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                return;
            }

            // Insert Token to DB
            boolean result = SqlHelpers.InsertToken(token, user.getUsername());
            if(!result){
                response.setStatusCode(HttpStatus.SC_CONFLICT);
                return;
            }
            // Set response body
            StringEntity entity = new StringEntity(
                    "{\"Token\": \"" + token + "\"}",
                    ContentType.create("application/json", Consts.UTF_8));
            response.setEntity(entity);
        }
        catch (JsonProcessingException e){
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            System.out.println("Error in JsonProcessing");
        }
        catch(Exception e){
            // Out of scope invalid input handling
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            return;
        }
    }
}

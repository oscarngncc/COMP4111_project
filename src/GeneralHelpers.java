import json.Book;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
/**
 * This the class of General Helper Functions.
 *
 */
public class GeneralHelpers {
    /**
     * Method to extract parameters from URL
     * @param url the url input
     * @return HashMap of parameter names and values .
     */
    public static Map<String, String> GetParamsMap(String url) throws UnsupportedEncodingException
    {
        String query = url.substring(url.indexOf("?")+1);
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {
            if (param.split("=").length == 2 ) {
                String name = param.split("=")[0].toUpperCase();
                String value = param.split("=")[1];
                value = URLDecoder.decode(value, "UTF-8");
                map.put(name, value);
            }
        }
        return map;
    }

    /**
     * Method to generate token with the username
     * @param id the user id in the username like "001"
     * @return String token.
     */
    public static String GenerateToken(String id)
    {
        String token = id;
        for (int i = id.length(); i < 12; i++) {
            token = token + (int) (Math.random() * (10));
        }
        return token;
    }

    /**
     * Method to extract book id from URL by regex
     * @param url the url input
     * @return book id.
     */
    public static int GetBookIdFromUrl(String url){
        try {
            var arr1 = url.split("/");
            String valStr = arr1[arr1.length - 1].split("\\?")[0];
            int id = Integer.parseInt(valStr);
            return id;
        } catch (NumberFormatException e){ e.printStackTrace(); return 0; }
    }
    /**
     * Method to get book info from params
     * @param params the params we get from Method GetParamsMap
     * @return Book object containing the values in params.
     */
    public static Book GetBookFromParams(Map<String, String> params){
        Book book = new Book();
        try{
            if(params.containsKey("ID"))
                book.setBookId(Integer.parseInt(params.get("ID")));
        }catch (NumberFormatException nfe)
        {
        }
        if(params.containsKey("TITLE"))
            book.setTitle(params.get("TITLE"));
        if(params.containsKey("AUTHOR"))
            book.setAuthor(params.get("AUTHOR"));
        return book;
    }
}

import json.Book;

import java.util.HashMap;
import java.util.Map;

public class GeneralHelpers {
    public static Map<String, String> GetParamsMap(String url)
    {
        String query = url.substring(url.indexOf("?")+1);
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {
            if (param.split("=").length == 2 ) {
                String name = param.split("=")[0];
                String value = param.split("=")[1];
                map.put(name, value);
            }
        }
        return map;
    }

    public static String GenerateToken(String id)
    {
        String token = id;
        for (int i = id.length(); i < 12; i++) {
            token = token + (int) (Math.random() * (10));
        }
        return token;
    }

    public static int GetBookIdFromUrl(String url){
        try {
            var arr1 = url.split("/");
            String valStr = arr1[arr1.length - 1].split("\\?")[0];
            int id = Integer.parseInt(valStr);
            return id;
        } catch (NumberFormatException e){ e.printStackTrace(); return 0; }
    }

    public static Book GetBookFromParams(Map<String, String> params){
        Book book = new Book();
        try{
            if(params.containsKey("id"))
                book.setBookId(Integer.parseInt(params.get("id")));
        }catch (NumberFormatException nfe)
        {
        }
        if(params.containsKey("title"))
            book.setTitle(params.get("title"));
        if(params.containsKey("author"))
            book.setAuthor(params.get("author"));
        if(params.containsKey("publisher"))
            book.setPublisher(params.get("publisher"));
        if(params.containsKey("year"))
            book.setYear(params.get("year"));
        if(params.containsKey("available"))
            if(params.get("available") == "true")
                book.setAvailable(true);
            else
                book.setAvailable(false);
        return book;
    }
}

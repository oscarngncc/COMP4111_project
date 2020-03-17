import json.Book;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralHelpers {
    public static Map<String, String> GetParamsMap(String url)
    {
        String query = url.substring(url.indexOf("?")+1);
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
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

    public static String GenerateTransactionId()
    {
        String tranactionId = "";
        for (int i = tranactionId.length(); i < 6; i++) {
            tranactionId = tranactionId + (int) (Math.random() * (10));
        }
        return tranactionId;
    }

    public static int GetBookIdFromUrl(String url){
        int id = 0;
        Pattern pattern = Pattern.compile("books*?");
        Matcher matcher = pattern.matcher(url);
        while (matcher.find()) {
            try{
                id = 3;
            }catch (NumberFormatException nfe)
            {
            }
        }
        return id;
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

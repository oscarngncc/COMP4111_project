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

    public static String GenerateTransactionId(String id)
    {
        String tranactionId = id;
        for (int i = id.length(); i < 12; i++) {
            tranactionId = tranactionId + (int) (Math.random() * (10));
        }
        return tranactionId;
    }
}

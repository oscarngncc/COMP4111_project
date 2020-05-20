import json.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;

import static org.junit.jupiter.api.Assertions.*;

class GeneralHelpersTest {

    @Test
    public void checkGetBookIdFromUrl(){
        String url1 = "http://localhost:8080/BookManagementService/books/123?token=asf3219";
        String url2 = "http://localhost:8080/BookManagementService/books/43?token=12798";
        assertEquals(123, GeneralHelpers.GetBookIdFromUrl(url1));
        assertEquals(43, GeneralHelpers.GetBookIdFromUrl(url2));
    }


    @Test
    public void checkGenerateToken(){
        String token = GeneralHelpers.GenerateToken("00001");
        String token1 = GeneralHelpers.GenerateToken("00015");
        String token1_1 = GeneralHelpers.GenerateToken("00015");

        assertTrue(token.contains("00001"));
        assertTrue(token1.contains("00015"));
        assertTrue( ! token1.equals(token1_1) );
        assertTrue(token.length()==token1.length() );
    }


    @Test
    public  void checkGetParamsMap() throws UnsupportedEncodingException {
        String url = "http://localhost:8080/BookManagementService/books?title=Alice&token=asf3219";
        String url1 = "http://localhost:8080/BookManagementService/books?author=Lewis&token=asf3219";
        String url2 = "http://localhost:8080/BookManagementService/books?author=Lewis&id=5&token=asf3219";

        var map = GeneralHelpers.GetParamsMap(url);
        var map1 = GeneralHelpers.GetParamsMap(url1);
        var map2 = GeneralHelpers.GetParamsMap(url2);

        assertTrue(map.containsKey("TITLE"));
        assertTrue( map.containsKey("TOKEN"));
        assertTrue(! map.containsKey(""));


        assertTrue(map1.containsKey("AUTHOR"));
        assertTrue(map2.containsKey("ID"));

        //Helper Class
        String url_empty = "http://localhost:8080/BookManagementService/books?";
        var empty = GeneralHelpers.GetParamsMap(url_empty);
        assertTrue(empty.isEmpty());
    }


    @Test
    public void checkGetBookFromParam() throws UnsupportedEncodingException {
        String url = "http://localhost:8080/BookManagementService/books?author=Lewis&id=5&token=asf3219";
        var map = GeneralHelpers.GetParamsMap(url);
        Book book = GeneralHelpers.GetBookFromParams(map);

        assertTrue(book.getAuthor().equals("Lewis") );
        assertTrue(book.getBookId() == 5 );

    }
}
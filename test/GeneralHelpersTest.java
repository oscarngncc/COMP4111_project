import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneralHelpersTest {


    @Test
    public void checkGetBookIdFromUrl(){
        String url1 = "http://localhost:8080/BookManagementService/books/123?token=asf3219";
        String url2 = "http://localhost:8080/BookManagementService/books/43?token=12798";
        assertEquals(123, GeneralHelpers.GetBookIdFromUrl(url1));
        assertEquals(43, GeneralHelpers.GetBookIdFromUrl(url2));
    }
}
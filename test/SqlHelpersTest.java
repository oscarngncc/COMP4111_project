import json.Book;
import json.BookList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SqlHelpersTest {

    @Test
    public void checkIsUserFound(){
        assertTrue(SqlHelpers.IsUserFound("user00001", "pass00001" ) );
        assertTrue(SqlHelpers.IsUserFound("user05613", "pass05613" ));
        assertTrue(SqlHelpers.IsUserFound("user10000", "pass10000" ));

        assertFalse(SqlHelpers.IsUserFound("", ""));
        assertFalse(SqlHelpers.IsUserFound("10000", "10000"));
    }

    @Test
    public void checkInsertDeleteToken(){
        String token = "sfdafda";
        assertTrue(SqlHelpers.InsertToken(token));
        assertTrue(SqlHelpers.IsTokenFound(token));

        assertTrue(SqlHelpers.DeleteToken(token));
        assertFalse(SqlHelpers.IsTokenFound(token));
    }


    @Test
    public void checkInsertLookUpDeleteBook(){
        Book book = new Book();
        book.setAuthor("Onion");
        book.setTitle("SqlHelperTest");
        book.setPublisher("Earth Ltd");
        book.setYear("1869");

        int id = SqlHelpers.InsertBook(book);
        assertFalse(id == 0);

        BookList list = SqlHelpers.LookUpBook(book, 10, "", true );
        assertEquals(list.getFoundBooks(), 1 );
        assertTrue( list.getResults().get(0).getTitle().equals(book.getTitle()) );


        assertTrue(SqlHelpers.DeleteBook(id));
        list = SqlHelpers.LookUpBook(book, 10, "", true );
        assertEquals(list.getFoundBooks(), 0 );
    }


    @Test
    public void checkLoanReturnBook(){

        // @return 10 if book not found;
        // 15 if book is in conflict status;
        // 20 if success

        Book book = new Book();
        book.setAuthor("Temper");
        book.setTitle("Life");
        book.setPublisher("Magic Ltd");
        book.setYear("2106");

        int id = SqlHelpers.InsertBook(book);
        assertFalse(id == 0);

        assertEquals( SqlHelpers.LoanBook(id), 20 );
        assertEquals( SqlHelpers.LoanBook(id), 15 );
        assertEquals( SqlHelpers.ReturnBook(id), 20 );
        assertEquals( SqlHelpers.ReturnBook(id), 15 );

        assertTrue(SqlHelpers.DeleteBook(id));
        assertEquals( SqlHelpers.LoanBook(id), 10 );
        assertEquals( SqlHelpers.ReturnBook(id), 10 );
    }



}




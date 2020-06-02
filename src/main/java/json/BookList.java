package json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class BookList {
    @JsonProperty("FoundBooks")
    private int foundBooks;

    public int getFoundBooks() {
        return foundBooks;
    }

    public void setFoundBooks(int foundBooks) {
        this.foundBooks = foundBooks;
    }

    @JsonProperty("Results")
    private List<Book> results = new ArrayList<Book>();

    public List<Book> getResults() {
        return results;
    }

    public void setResults(List<Book> results) {
        this.results = results;
    }

    public void AddResult(Book result) {
        this.results.add(result);
    }
}

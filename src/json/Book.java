package json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Book {
    @JsonProperty("Book")
    private int bookId;

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Author")
    private String author;

    @JsonProperty("Publisher")
    private String publisher;

    @JsonProperty("Year")
    private String year;

    @JsonProperty("Available")
    private Boolean available;
}

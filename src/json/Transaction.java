package json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {
    @JsonProperty("Transaction")
    private int transactionId;

    public int getTransactionId() {
        return transactionId;
    }

    @JsonProperty("Book")
    private int bookId;

    public int getBookId() {
        return bookId;
    }

    @JsonProperty("Action")
    private String action;

    public String getAction() {
        return action;
    }

    @JsonProperty("Operation")
    private String Operation;

    public String getOperation() {
        return Operation;
    }
}

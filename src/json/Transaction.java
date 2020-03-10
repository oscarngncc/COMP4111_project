package json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {
    @JsonProperty("Transaction")
    private String transactionId;

    public String getTransactionId() {
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

    @JsonProperty("operation")
    private String Operation;

    public String getOperation() {
        return Operation;
    }
}

package json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Availability {
    @JsonProperty("Available")
    private boolean available;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isAvailable() {
        return available;
    }
}

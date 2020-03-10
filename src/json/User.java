package json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    @JsonProperty("Username")
    private String username;
    public String getUsername() {
        return username;
    }

    @JsonProperty("Password")
    private String password;
    public String getPassword() {
        return password;
    }
}

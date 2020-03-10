package json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    public User( String username, String password){
        this.username = username;
        this.password = password;
    }

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

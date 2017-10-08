package helloworld.classform;

import javax.validation.constraints.NotNull;

public class InfoUser{
    @NotNull
    private String dbUsername = "root";
    private String dbPassword = "664998196";
    private String dbServer = "localhost";
    public InfoUser(){}


    public String getdbUsername() {
        return this.dbUsername;
    }

    public void setDbUsername(String name) {
        this.dbUsername = name;
    }

    public String getDbServer(){return this.dbServer;}

    public String getDbPassword(){return this.dbPassword;}
    public String toString() {
        return String.format("User: %s server: %s", dbUsername, getDbServer());
    }
}

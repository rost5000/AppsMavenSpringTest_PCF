package helloworld.classform;

public class Greeting {

    private final long id;
    private final String content;
    private String someString;
    public Greeting(long id, String content, String someString) {
        this.id = id;
        this.content = content;
        this.someString = someString;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public String getSomeString(){return  someString;}
}
package helloworld.controller;
import java.util.concurrent.atomic.AtomicLong;

import helloworld.classform.Greeting;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @Value("${config.name}")
    String someString;

    @RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value="content", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name), someString);
    }
}
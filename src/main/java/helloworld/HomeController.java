package helloworld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


import javax.sql.DataSource;
import javax.validation.Valid;

import helloworld.classform.ChoosenValue;
import helloworld.classform.InfoUser;
import helloworld.classform.PersonForm;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.facebook.api.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    public final org.slf4j.Logger log =  LoggerFactory.getLogger(HomeController.class);
    @Autowired(required = false) DataSource dataSource;
    @Autowired(required = false) RedisConnectionFactory redisConnectionFactory;
    @Autowired(required = false) MongoDbFactory mongoDbFactory;
    @Autowired(required = false) ConnectionFactory rabbitConnectionFactory;

    @Autowired(required = false) ApplicationInstanceInfo instanceInfo;


    @RequestMapping("/home")
    public String home(Model model) {
        model.addAttribute("instanceInfo", instanceInfo);

        if (instanceInfo != null) {
            Map<Class<?>, String> services = new LinkedHashMap<Class<?>, String>();
            services.put(dataSource.getClass(), toString(dataSource));
            services.put(mongoDbFactory.getClass(), toString(mongoDbFactory));
            services.put(redisConnectionFactory.getClass(), toString(redisConnectionFactory));
            services.put(rabbitConnectionFactory.getClass(), toString(rabbitConnectionFactory));
            model.addAttribute("services", services.entrySet());
        }

        return "home";
    }

    @GetMapping("/")
    public String showForm(Model model, PersonForm personForm) {
        return "index";
    }


    @RequestMapping("/formValid")
    public String checkPersonInfo(Model model, @Valid PersonForm personForm, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "formValid";
        }
        //Если валидация прошла успешно
        model.addAttribute("personInfo", personForm);
        return "formValid";
    }

    /**
     * Пытался оеализовать через Функцию -- не получилось, в примере показан был реализован класс
     * Да, я знаю, что это вложенный класс
     */
    @Controller
    @RequestMapping("/facebookAuth")
    public class TestAuthFacebook {
        ConnectionRepository connectionRepository;
        Facebook facebook;

        TestAuthFacebook(Facebook facebook, ConnectionRepository connectionRepository){
            this.connectionRepository = connectionRepository;
            this.facebook = facebook;
        }
        @GetMapping
        @PostMapping
        public String authFacebook(Model model) {
            if (connectionRepository.findPrimaryConnection(Facebook.class) == null) {
                return "redirect:/connect/facebook";
            }
            //* //Эта ересь не поддерживается текущей библиотекой, необходимо вручную доставать данные
            String [] fields = { "id", "email",  "first_name", "last_name" };
            User userProfile = facebook.fetchObject("me", User.class, fields);
            model.addAttribute("facebookProfile", userProfile);

            PagedList<Post> feed = facebook.feedOperations().getFeed();
            //*/
            model.addAttribute("feed", feed);
            return "authWithFacebook";
        }
    };

    @Controller
    public class ShowTable{
        JdbcTemplate jdbcTemplate;
        InfoUser infoUser = null;
        String schema = null;
;

        public ShowTable(){}

        @PostMapping("/showSchemas")
        public String showInfo(Model model, @Valid InfoUser info, @Valid ChoosenValue choosenValue,
                               BindingResult bindingResult){
            if(!bindingResult.hasErrors())
                this.infoUser = info;
            return "redirect:/showTable";
        }

        @GetMapping("/showSchemas")
        public String showInfo1(Model model, @Valid ChoosenValue choosenValue, BindingResult bindingResult){

            /**
             * ТО, что я сейчас делаю -- это неправильно: Spring сама должна инициализировать переменные,
             * иначе все эти переменные будут жить в сборщике мусора.
             *
             * Мне нужно инициализировать переменные вручную для дебага
             *
            try {
                if(infoUser == null ) {
                    jdbcTemplate = new JdbcTemplate(dataSource);
                    model.addAttribute("showFormUser", "1");
                }else {
                    Connection connection = DriverManager.getConnection(infoUser.getDbServer(),
                            infoUser.getdbUsername(), infoUser.getDbPassword());
                    jdbcTemplate = new JdbcTemplate(new SingleConnectionDataSource(connection, false));

                }//*/
                jdbcTemplate = new JdbcTemplate(dataSource);



            model.addAttribute("data", getSchemas());
                model.addAttribute("formAction1", "/showTable");
            return "showTable";
        }

        @RequestMapping("/showTable")
        String getTable(Model model){
            if(schema == null)
                return "redirect:/showSchemas";
            model.addAttribute("data", getTable());
            model.addAttribute("formAction1", "/showTable");
            return "showTable";

        }

        List getSchemas(){
            LinkedList<String>list = new LinkedList<String>();
            jdbcTemplate.query("SHOW SCHEMAS", new Object[]{},
                    (rs, key)->list.add(rs.getString(1))).forEach(
                            tmpl->log.info(list.getLast()));
            return list;
        }

        List getTable(){
            LinkedList<String>list = new LinkedList<String>();
            jdbcTemplate.query("SHOW TABLE FROM %1", new Object[]{schema},
                    (rs, key)->list.add(rs.getString(1))).forEach(
                            tmpl->log.info(list.getLast())
            );
            return list;
        }

    }

    private String toString(DataSource dataSource) {
        if (dataSource == null) {
            return "<none>";
        } else {
            try {
                Field urlField = ReflectionUtils.findField(dataSource.getClass(), "url");
                ReflectionUtils.makeAccessible(urlField);
                return stripCredentials((String) urlField.get(dataSource));
            } catch (Exception fe) {
                try {
                    Method urlMethod = ReflectionUtils.findMethod(dataSource.getClass(), "getUrl");
                    ReflectionUtils.makeAccessible(urlMethod);
                    return stripCredentials((String) urlMethod.invoke(dataSource, (Object[])null));
                } catch (Exception me){
                    return "<unknown> " + dataSource.getClass();
                }
            }
        }
    }

    private String toString(MongoDbFactory mongoDbFactory) {
        if (mongoDbFactory == null) {
            return "<none>";
        } else {
            try {
                return mongoDbFactory.getDb().getMongo().getAddress().toString();
            } catch (Exception ex) {
                return "<invalid address> " + mongoDbFactory.getDb().getMongo().toString();
            }
        }
    }

    private String toString(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            return "<none>";
        } else {
            if (redisConnectionFactory instanceof JedisConnectionFactory) {
                JedisConnectionFactory jcf = (JedisConnectionFactory) redisConnectionFactory;
                return jcf.getHostName().toString() + ":" + jcf.getPort();
            } else if (redisConnectionFactory instanceof LettuceConnectionFactory) {
                LettuceConnectionFactory lcf = (LettuceConnectionFactory) redisConnectionFactory;
                return lcf.getHostName().toString() + ":" + lcf.getPort();
            }
            return "<unknown> " + redisConnectionFactory.getClass();
        }
    }

    private String toString(ConnectionFactory rabbitConnectionFactory) {
        if (rabbitConnectionFactory == null) {
            return "<none>";
        } else {
            return rabbitConnectionFactory.getHost() + ":"
                    + rabbitConnectionFactory.getPort();
        }
    }

    private String stripCredentials(String urlString) {
        try {
            if (urlString.startsWith("jdbc:")) {
                urlString = urlString.substring("jdbc:".length());
            }
            URI url = new URI(urlString);
            return new URI(url.getScheme(), null, url.getHost(), url.getPort(), url.getPath(), null, null).toString();
        }
        catch (URISyntaxException e) {
            System.out.println(e);
            return "<bad url> " + urlString;
        }
    }

}



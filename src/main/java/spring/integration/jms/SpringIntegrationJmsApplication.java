package spring.integration.jms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;

/**
 *
 * @author rafael.guterres
 */
@SpringBootApplication
@PropertySource("classpath:META-INF/spring/integration/application.properties")
@ImportResource("classpath*:META-INF/spring/integration/application.xml")
public class SpringIntegrationJmsApplication {
    public static void main(String[] args) throws Exception {
        new SpringApplication(SpringIntegrationJmsApplication.class).run(args);
        System.out.println("SpringIntegrationJmsApplication RUNNING...");
    }
}

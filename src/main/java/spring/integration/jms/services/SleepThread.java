package spring.integration.jms.services;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;


/**
 *
 * @author rafael.guterres
 */
@Service
public class SleepThread {

    public Message<String> waiting(Message<String> message) {
        System.out.println("spring.integration.jms.services.SleepThread.waiting()");
        return message;
    }
}

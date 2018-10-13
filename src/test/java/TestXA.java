
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.jta.WebLogicJtaTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 *
 * @author guter
 */
public class TestXA {

    Context context;
    WebLogicJtaTransactionManager transactionManager;
    
    @Before
    public void setUp() throws Exception {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "weblogic.jndi.WLInitialContextFactory");
        environment.put(Context.PROVIDER_URL, "t3://localhost:7001");
        context = new InitialContext(environment);
    }
    
    private ConnectionFactory createConnectionFactory() {
        try {
            return (ConnectionFactory) context.lookup("jms.ConnectionFactory1");
        } catch (NamingException ex) {
            return null;
        }
    }
    
    //@Test
    public void testSpring() throws Exception {
        ConnectionFactory cf = createConnectionFactory();
        JmsTemplate jms = new JmsTemplate(cf);
        jms.setDefaultDestinationName("jms/Queue1");
        jms.setReceiveTimeout(100);

        jms.convertAndSend("Hello");
        Object msg = jms.receiveAndConvert();
        Assert.assertEquals("Hello", msg);
    }
    
    //@Test
    public void testSpringXa() throws Exception {
        ConnectionFactory connectionFactory = createConnectionFactory();

        JmsTemplate jms = new JmsTemplate(connectionFactory);
        jms.setDefaultDestinationName("jms.Queue1");
        jms.setReceiveTimeout(100);
        TransactionTemplate xaTx = new TransactionTemplate(transactionManager);

        xaTx.execute(ts -> {
            jms.convertAndSend("Hello");
            return null;
        });
        Object msg = xaTx.execute(ts -> jms.receiveAndConvert());
        Assert.assertEquals("Hello", msg);

        xaTx = new TransactionTemplate(transactionManager);
        xaTx.execute(ts -> {
            jms.convertAndSend("Hello");
            ts.setRollbackOnly();
            return null;
        });
        msg = xaTx.execute(ts -> jms.receiveAndConvert());
        Assert.assertNull(msg);
    }
    
    //@Test
    public void testSpringReceiverXa() throws Exception {

        ConnectionFactory cf = createConnectionFactory();

        DefaultJmsListenerContainerFactory jlcf = new DefaultJmsListenerContainerFactory();
        jlcf.setTransactionManager(transactionManager);
        jlcf.setSessionTransacted(true);
        jlcf.setConnectionFactory(cf);

        AtomicReference<Message> holder = new AtomicReference<>();

        SimpleJmsListenerEndpoint jle = new SimpleJmsListenerEndpoint();
        jle.setDestination("jms.Queue1");
        jle.setMessageListener(message -> {
            synchronized (holder) {
                holder.set(message);
                holder.notifyAll();
            }
        });
        DefaultMessageListenerContainer mlc = jlcf.createListenerContainer(jle);
        mlc.initialize();
        mlc.start();

        JmsTemplate jms = new JmsTemplate(cf);
        jms.setDefaultDestinationName("jms.Queue1");

        synchronized (holder) {
            jms.convertAndSend("Hello");
            holder.wait();
        }
        Assert.assertNotNull(holder.get());

    }
}

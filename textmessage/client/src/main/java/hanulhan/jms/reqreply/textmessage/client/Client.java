package hanulhan.jms.reqreply.textmessage.client;

import static java.lang.Thread.sleep;
import org.apache.activemq.ActiveMQConnectionFactory;
 
import javax.jms.*;
import java.util.Random;
import java.util.Scanner;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
 
public class Client implements MessageListener {
    private static int ackMode;
    private static String clientQueueName;
 
    private boolean transacted = false;
    private MessageProducer producer;
    private static final Logger LOGGER = Logger.getLogger(Client.class);
 
    static {
        clientQueueName = "client.messages";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }
 
    public Client() throws InterruptedException {

//        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://192.168.21.10:61616");
//        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://192.168.1.61:61616");
//        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://192.168.178.80:61616");
        Connection connection;
        int msgCount = 0;
        try {
            LOGGER.log(Level.DEBUG, "Start Client,  Broker: " + connectionFactory.getBrokerURL());
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(transacted, ackMode);
            Destination adminQueue = session.createQueue(clientQueueName);
 
            //Setup a message producer to send message to the queue the server is consuming from
            this.producer = session.createProducer(adminQueue);
            this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
 
            //Create a temporary queue that this client will listen for responses on then create a consumer
            //that consumes message from this temporary queue...for a real application a client should reuse
            //the same temp queue for each message to the server...one temp queue per client
            Destination tempDest = session.createTemporaryQueue();
            MessageConsumer responseConsumer = session.createConsumer(tempDest);
 
            //This class will handle the messages to the temp queue as well
            responseConsumer.setMessageListener(this);
 
            //Now create the actual message you want to send
            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText("MyProtocolMessage");
 
            //Set the reply to field to the temp queue you created above, this is the queue the server
            //will respond to
            txtMessage.setJMSReplyTo(tempDest);
 
            //Set a correlation ID so when you get a response you know which sent message the response is for
            //If there is never more than one outstanding message to the server then the
            //same correlation ID can be used for all the messages...if there is more than one outstanding
            //message to the server you would presumably want to associate the correlation ID with this
            //message somehow...a Map works good
            String correlationId = this.createRandomString();
            txtMessage.setJMSCorrelationID(correlationId);

            // ##########################################
            Boolean terminate = false;
            Scanner keyboard = new Scanner(System.in);

            while (terminate == false) {
                sleep(100);
                LOGGER.log(Level.INFO, "Press any key + <Enter> to continue and x + <Enter> to exit");
                String input = keyboard.nextLine();
                if (input != null) {
                    if ("x".equals(input)) {
                        LOGGER.log(Level.INFO, "Exit program");
                        terminate = true;
                    } else {
                        msgCount++;
                        correlationId = this.createRandomString();
                        txtMessage.setJMSCorrelationID(correlationId);

//                        txtMessage.setText("Message " + msgCount + " from Client " + clientId);
                        txtMessage.setText("Message " + msgCount);
                        LOGGER.log(Level.TRACE, "Send Message (" + correlationId + "): " + txtMessage.getText());
                        producer.send(txtMessage);
                    }

                }
            }
            session.close();
            connection.close();
            
            
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, e);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.ERROR, ex);
        }
    }
 
    private String createRandomString() {
        Random random = new Random(System.currentTimeMillis());
        long randomLong = random.nextLong();
        return Long.toHexString(randomLong);
    }
 
    public void onMessage(Message message) {
        String messageText = null;
        try {
            if (message instanceof TextMessage) {

                TextMessage textMessage = (TextMessage) message;
                messageText = textMessage.getText();
                LOGGER.log(Level.DEBUG, "Client received message: [" + messageText + "]");
            }
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, e);
        }
    }
 
    public static void main(String[] args) throws InterruptedException {
        new Client();
    }
}
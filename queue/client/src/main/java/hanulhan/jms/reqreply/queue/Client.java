package hanulhan.jms.reqreply.queue;

import static java.lang.Thread.sleep;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.Random;
import java.util.Scanner;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Client implements MessageListener {

    private boolean transacted = false;
    private int clientId;
    private Boolean doReply;
    private MessageProducer producer;
    ActiveMQConnectionFactory connectionFactory;
    Session session;
    private static final Logger LOGGER = Logger.getLogger(Client.class);

    public Client(int aId, Boolean aDoReply) throws InterruptedException {

        clientId= aId;
        doReply = aDoReply;
        TextMessage txtMessage;
        String correlationId;
                
               
        connectionFactory = new ActiveMQConnectionFactory(Settings.MESSAGE_BROKER_URL);

        Connection connection;
        int msgCount = 0;
        try {
            LOGGER.log(Level.DEBUG, "Start Client(" + clientId + "),  Broker: " + connectionFactory.getBrokerURL());
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(transacted, Settings.CLIENT_ACK_MODE);
            Destination adminQueue = session.createQueue(Settings.MESSAGE_QUEUE_NAME);

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
                        //Now create the actual message you want to send
                        txtMessage = session.createTextMessage();
                        txtMessage.setJMSCorrelationID(correlationId);
                        if (aDoReply)   {
                            txtMessage.setJMSReplyTo(tempDest);
                        } else {
                            txtMessage.setJMSReplyTo(null);
                        }
                        txtMessage.setText("Message " + msgCount + " from Client " + clientId);

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
        Random random;
        random = new Random(System.currentTimeMillis());
        long randomLong = random.nextLong();
        return Long.toHexString(randomLong);
    }

    @Override
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
        int myId = 1;
        Boolean myDoReply = true;

        if (args.length > 0 && args[0].equals("?")) {
            LOGGER.log(Level.TRACE, "Client <clientId|?> [<doReply=true|false>]");
        } else if (args.length > 0) {
            myId = Integer.parseInt(args[0]);
            if (args.length > 1) {
                myDoReply = Boolean.parseBoolean(args[1]);
            }

        }

        Client client = new Client(myId, myDoReply);
    }
}

package hanulhan.jms.reqreply.topic;

import static java.lang.Thread.sleep;
import java.util.Date;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.Random;
import java.util.Scanner;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class PollingClient implements MessageListener {

    private boolean transacted = false;
    private int clientId;
    private Boolean doReply;
    private MessageProducer producer;
    ActiveMQConnectionFactory connectionFactory;
    Session session;
    private static final Logger LOGGER = Logger.getLogger(PollingClient.class);
    private final int WAIT_FOR_ACK_MILLI_SECONDS = 1000;
    private final int WAIT_FOR_RESPONSE_MILLI_SECONDS = 1000;

    public PollingClient(int aId, Boolean aDoReply) throws InterruptedException {

        clientId = aId;
        doReply = aDoReply;
        TextMessage txtMessage;
        String correlationId;
        int myMilliSeconds;
        TextMessage receiveMessage;
        Date myStartTime;
        String messageText = null;

        connectionFactory = new ActiveMQConnectionFactory(Settings.MESSAGE_BROKER_URL);

        Connection connection;
        int msgCount = 0;
        try {
            LOGGER.log(Level.DEBUG, "Start Client(" + clientId + "),  Broker: " + connectionFactory.getBrokerURL());
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(transacted, Settings.REP_ACK_MODE);
            Destination adminQueue = session.createTopic(Settings.MESSAGE_TOPIC_NAME);

            //Setup a message producer to send message to the queue the server is consuming from
            this.producer = session.createProducer(adminQueue);
            this.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            //Create a temporary queue that this client will listen for responses on then create a consumer
            //that consumes message from this temporary queue...for a real application a client should reuse
            //the same temp queue for each message to the server...one temp queue per client
            Destination tempDest = session.createTemporaryQueue();
            MessageConsumer responseConsumer = session.createConsumer(tempDest);

            //This class will handle the messages to the temp queue as well
            //responseConsumer.setMessageListener(this);
            // ##########################################
            Boolean terminate = false;
            Scanner keyboard = new Scanner(System.in);

            while (terminate == false) {

                sleep(1000);
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
                        if (aDoReply) {
                            txtMessage.setJMSReplyTo(tempDest);
                        } else {
                            txtMessage.setJMSReplyTo(null);
                        }
                        txtMessage.setText("Message " + msgCount + " from Client " + clientId);

                        LOGGER.log(Level.TRACE, "Send Message (" + correlationId + "): " + txtMessage.getText());
                        producer.send(txtMessage);

                        if (aDoReply) {
                            // Wait for response
                            myStartTime = new Date();
                            Message myMessage1 = null;

                            myMessage1 = responseConsumer.receive(WAIT_FOR_ACK_MILLI_SECONDS);

                            myMilliSeconds = (int) ((new Date().getTime() - myStartTime.getTime()));
                            if (myMessage1 != null) {
                                if (myMessage1 instanceof TextMessage) {
                                    receiveMessage = (TextMessage) myMessage1;
                                    messageText = receiveMessage.getText();
                                    LOGGER.log(Level.DEBUG, "Client receive [" + messageText + "] in " + myMilliSeconds + "ms, waiting for more");
                                }
                                
                                myStartTime = new Date();
                                Message myMessage2 = null;
                                myMessage2 = responseConsumer.receive(5000);
                                myMilliSeconds = (int) ((new Date().getTime() - myStartTime.getTime()));

                                if (myMessage2 != null) {
                                    if (myMessage2 instanceof TextMessage) {
                                        receiveMessage = (TextMessage) myMessage2;
                                        messageText = receiveMessage.getText();
                                        LOGGER.log(Level.DEBUG, "Client receive [" + messageText + "] in " + myMilliSeconds + ", finished");
                                    }
                                } else {
                                    LOGGER.log(Level.DEBUG, "No Response received within " + WAIT_FOR_RESPONSE_MILLI_SECONDS + " ms");
                                }

                            } else {
                                LOGGER.log(Level.DEBUG, "No ACK received within " + WAIT_FOR_ACK_MILLI_SECONDS + " ms");
                            }
                                

                        }

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

        PollingClient client = new PollingClient(myId, myDoReply);
    }
}

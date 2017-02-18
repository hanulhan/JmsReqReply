/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hanulhan.jms.reqreply.textmessage.client;

import hanulhan.jms.reqreply.textmessage.util.Settings;
import java.util.Random;
import java.util.Scanner;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author uhansen
 */
public class Client implements MessageListener {

    private static final Logger LOGGER = Logger.getLogger(Client.class);

    private boolean transacted = false;
    private MessageProducer producer;

    public Client(int aId, Boolean aDoReply) {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(Settings.MESSAGE_BROKER_URL);
        Connection connection;
        int msgCount= 1;
        try {
            LOGGER.log(Level.TRACE, "Start Client");
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(transacted, Settings.CLIENT_ACK_MODE);
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

            //Now create the actual message you want to send
            TextMessage txtMessage = session.createTextMessage();
            txtMessage.setText("MyProtocolMessage " + msgCount);

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
            LOGGER.log(Level.TRACE, "Send Message: " + txtMessage.toString());
            this.producer.send(txtMessage);

            Boolean terminate = false;
            Scanner keyboard = new Scanner(System.in);

            while (terminate == false) {
                LOGGER.log(Level.INFO, "Press any key + <Enter> to continue and x + <Enter> to exit");
                String input = keyboard.nextLine();
                if (input != null) {
                    if ("x".equals(input)) {
                        LOGGER.log(Level.INFO, "Exit program");
                        terminate = true;
                    } else {
                        msgCount++;
                        txtMessage.setText("MyProtocolMessage " + msgCount);
                        LOGGER.log(Level.TRACE, "Send Message: " + txtMessage.toString());
                        this.producer.send(txtMessage);
                    }

                }
            }
            session.close();
            connection.close();

        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, e);
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
                LOGGER.log(Level.TRACE, "Receive Message: " + messageText);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, "JMSException," + e);
        }
    }

    public static void main(String[] args) {
        int myId = 1;
        Boolean myDoReply = true;

        if (args.length > 0 && args[0].equals("?")) {
            LOGGER.log(Level.TRACE, "JmsRequRepylClient <clientId|?> [<doReply=true|false>]");
        } else if (args.length > 0) {
            myId = Integer.parseInt(args[0]);
            if (args.length > 1) {
                myDoReply = Boolean.parseBoolean(args[1]);
            }

        }

        Client client = new Client(myId, myDoReply);
    }

}

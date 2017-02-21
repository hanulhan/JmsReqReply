/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hanulhan.jms.reqreply.textmessage.server;

import hanulhan.jms.reqreply.textmessage.util.Settings;
import java.util.Scanner;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 *
 * @author uhansen
 */
public class Server implements MessageListener {

    private ActiveMQConnectionFactory connectionFactory;
    private Session session;
    private Connection connection;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private Destination adminQueue;
    private MessageConsumer consumer;
    private final int serverId;
    private static final Logger LOGGER = Logger.getLogger(Server.class);

    public Server(int aServerId) {
        serverId = aServerId;
        Boolean terminate = false;
        Scanner keyboard = new Scanner(System.in);

        //Delegating the handling of messages to another class, instantiate it before setting up JMS so it
        //is ready to handle messages
        this.setupMessageQueueConsumer();
        while (terminate == false) {
            LOGGER.log(Level.INFO, "Press x + <Enter> to terminate the Server");
            String input = keyboard.nextLine();
            if (input != null) {
                if ("x".equals(input)) {
                    terminate = true;
                }
            }
        }
    }

    public static void startBroker() {
        try {
            //This message broker is embedded
            LOGGER.log(Level.INFO, "Server:startBroker()");
            BrokerService broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.addConnector(Settings.MESSAGE_BROKER_URL);
            broker.start();
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "JMS Exception: " + e);
        }
    }

    private void setupMessageQueueConsumer() {
        connectionFactory = new ActiveMQConnectionFactory(Settings.MESSAGE_BROKER_URL);

        try {
            connection = connectionFactory.createConnection();

            session = connection.createSession(this.transacted, Settings.SERVER_ACK_MODE);
            adminQueue = this.session.createQueue(Settings.MESSAGE_QUEUE_NAME);

            //Setup a message producer to respond to messages from clients, we will get the destination
            //to send to from the JMSReplyTo header field from a Message
            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            //Set up a consumer to consume messages off of the admin queue
            consumer = this.session.createConsumer(adminQueue);
            consumer.setMessageListener(this);
            connection.start();
            LOGGER.log(Level.INFO, "Starting Server(" + serverId + "), Broker: " + connectionFactory.getBrokerURL());
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, "JMS Exception: " + e);
        }
    }


    public void close() {
        try {
            LOGGER.log(Level.INFO, "Terminate Server");
            session.close();
            connection.close();
        } catch (JMSException jMSException) {
            LOGGER.log(Level.ERROR, "JMS Exception: " + jMSException);
        }

    }

    public static void main(String[] args) {
        Boolean startBroker = true;
        int myServerId = 1;
        Server myServer = null;

        LOGGER.log(Level.TRACE, "Server:main()");
        if (args.length > 0) {
            if ("?".equals(args[0])) {
                LOGGER.log(Level.INFO, "java -jar Server serverId, [startBroker=true|false]");
            } else {
                myServerId = Integer.parseInt(args[0]);
            }

            if (args.length == 2 && Boolean.parseBoolean(args[1]) == false) {
                startBroker = false;
            }
        }

        if (startBroker) {
            LOGGER.log(Level.TRACE, "Start Broker");
            Server.startBroker();
        }
                    Server.startBroker();
        LOGGER.log(Level.TRACE, "Start Server");
        myServer = new Server(myServerId);

        myServer.close();

    }

    @Override
    public void onMessage(Message msg) {
                try {
            TextMessage response = this.session.createTextMessage();
            if (msg instanceof TextMessage) {

                TextMessage txtMsg = (TextMessage) msg;
                String messageText = txtMsg.getText();
                LOGGER.log(Level.TRACE, "Server(" + serverId + ") received message[" + messageText + "]");

//                Scanner keyboard = new Scanner(System.in);
//                String input = keyboard.nextLine();
                //message.acknowledge();
                //LOGGER.log(Level.INFO, "Send ACK"); 
                response.setText("Server(" + serverId + ") response msg[" + messageText + "]");
            }
            if (msg.getJMSReplyTo() != null) {
                //Set the correlation ID from the received message to be the correlation id of the response message
                //this lets the client identify which message this is a response to if it has more than
                //one outstanding message to the server
                response.setJMSCorrelationID(msg.getJMSCorrelationID());

                //Send the response to the Destination specified by the JMSReplyTo field of the received message,
                //this is presumably a temporary queue created by the client
                this.replyProducer.send(msg.getJMSReplyTo(), response);
            }
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, "JMS Exception: " + e);
        }
    }

}

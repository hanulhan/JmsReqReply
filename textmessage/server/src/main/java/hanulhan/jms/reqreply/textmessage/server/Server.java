/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */package hanulhan.jms.reqreply.textmessage.server;

import hanulhan.jms.reqreply.textmessage.util.MessageProtocol;
import java.util.Scanner;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Server implements MessageListener {

    private static int ackMode;
    private static String messageQueueName;
    private static String messageBrokerUrl;
    private static Boolean startBroker;

    private Session session;
    Connection connection;
    private boolean transacted = false;
    private MessageProducer replyProducer;
    private MessageProtocol messageProtocol;
    private static final Logger LOGGER = Logger.getLogger(Server.class);

    static {
//        messageBrokerUrl = "tcp://localhost:61616";
        startBroker = false;
        messageBrokerUrl = "tcp://192.168.21.10:61616";
//        messageBrokerUrl = "tcp://192.168.1.61:61616";
//        messageBrokerUrl = "tcp://localhost:61616";
//        messageBrokerUrl = "tcp://192.168.178.80:61616";

        messageQueueName = "client.messages";
        ackMode = Session.AUTO_ACKNOWLEDGE;
    }

    public Server() {

        Boolean terminate = false;
        Scanner keyboard = new Scanner(System.in);

        LOGGER.log(Level.TRACE, "Server:Server()");
        if (startBroker) {
            startBroker();
        }

        //Delegating the handling of messages to another class, instantiate it before setting up JMS so it
        //is ready to handle messages
        this.messageProtocol = new MessageProtocol();

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
        LOGGER.log(Level.INFO, "Terminate Server");
        try {
            session.close();
            connection.close();
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, "JMS Exception: " + e);
        }
    }

    public static void startBroker() {
        try {
            //This message broker is embedded
            LOGGER.log(Level.INFO, "Server:startBroker()");
            BrokerService broker = new BrokerService();
            broker.setPersistent(false);
            broker.setUseJmx(false);
            broker.addConnector(messageBrokerUrl);
            broker.start();
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "JMS Exception: " + e);
        }
    }

    private void setupMessageQueueConsumer() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(messageBrokerUrl);
        try {
            LOGGER.log(Level.TRACE, "Server::setupMessageQueueConsumer()");
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, ackMode);
            Destination adminQueue = this.session.createQueue(messageQueueName);

            //Setup a message producer to respond to messages from clients, we will get the destination
            //to send to from the JMSReplyTo header field from a Message
            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            //Set up a consumer to consume messages off of the admin queue
            MessageConsumer consumer = this.session.createConsumer(adminQueue);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, e);
        }
    }

    public void onMessage(Message message) {
        try {
            TextMessage response = this.session.createTextMessage();
            LOGGER.log(Level.TRACE, "Server::onMessage()");
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage) message;
                String messageText = txtMsg.getText();
                LOGGER.log(Level.TRACE, "Server received TextMessage[" + messageText + "]");
                response.setText( "Server reply for [" + messageText  + "]" );
                LOGGER.log(Level.INFO, "Press x + <Enter> to terminate the Server");
            }

            //Set the correlation ID from the received message to be the correlation id of the response message
            //this lets the client identify which message this is a response to if it has more than
            //one outstanding message to the server
            response.setJMSCorrelationID(message.getJMSCorrelationID());

            //Send the response to the Destination specified by the JMSReplyTo field of the received message,
            //this is presumably a temporary queue created by the client
            this.replyProducer.send(message.getJMSReplyTo(), response);
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, e);
        }
    }

//    public void close() {
//        try {
//            LOGGER.log(Level.INFO, "Terminate Server");
//            session.close();
//            connection.close();
//        } catch (JMSException jMSException) {
//            LOGGER.log(Level.ERROR, "JMS Exception: " + jMSException);
//        }
//
//    }
    public static void main(String[] args) {
        new Server();

    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hanulhan.jms.reqreply.topic;

import hanulhan.jms.reqreply.textmessage.util.Settings;
import static java.lang.Thread.sleep;
import java.util.Scanner;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class Server implements MessageListener {

    private ActiveMQConnectionFactory connectionFactory;
    private Session session;
    private Connection connection;
    private int serverId;

    private boolean transacted = false;
    private MessageProducer replyProducer;
    private static final Logger LOGGER = Logger.getLogger(Server.class);

    public Server(int aServerId) {
        serverId = aServerId;
        Boolean terminate = false;
        Scanner keyboard = new Scanner(System.in);

        LOGGER.log(Level.TRACE, "Start Server(id: " + serverId + ")");

        //Delegating the handling of messages to another class, instantiate it before setting up JMS so it
        //is ready to handle messages
//        this.setupMessageQueueConsumer();
        this.setupMessageTopicConsumer();
        while (terminate == false) {
            LOGGER.log(Level.INFO, "Press x + <Enter> to terminate the Server");
            String input = keyboard.nextLine();
            if (input != null) {
                if ("x".equals(input)) {
                    terminate = true;
                }
            }
        }

        this.close();
    }

    public void close() {
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
            broker.addConnector(Settings.MESSAGE_BROKER_URL);
            broker.start();
        } catch (Exception e) {
            LOGGER.log(Level.ERROR, "JMS Exception: " + e);
        }
    }

    private void setupMessageQueueConsumer() {
        connectionFactory = new ActiveMQConnectionFactory(Settings.MESSAGE_BROKER_URL);
        try {
            LOGGER.log(Level.TRACE, "Server::setupMessageQueueConsumer()");
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, Settings.REQ_ACK_MODE);
            Destination adminQueue = this.session.createQueue(Settings.MESSAGE_TOPIC_NAME);

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

        private void setupMessageTopicConsumer() {
        connectionFactory = new ActiveMQConnectionFactory(Settings.MESSAGE_BROKER_URL);
        try {
            LOGGER.log(Level.TRACE, "Server::setupMessageTopicConsumer()");
            connection = connectionFactory.createConnection();
            connection.start();
            this.session = connection.createSession(this.transacted, Settings.REQ_ACK_MODE);
            Destination adminTopic = this.session.createTopic(Settings.MESSAGE_TOPIC_NAME);

            //Setup a message producer to respond to messages from clients, we will get the destination
            //to send to from the JMSReplyTo header field from a Message
            this.replyProducer = this.session.createProducer(null);
            this.replyProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            //Set up a consumer to consume messages off of the admin queue
            MessageConsumer consumer = this.session.createConsumer(adminTopic);
            consumer.setMessageListener(this);
        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, e);
        }
    }
    
    @Override
    public void onMessage(Message message) {
        try {
            LOGGER.log(Level.TRACE, "Server::onMessage()");
            if (message instanceof TextMessage) {
                TextMessage txtMsg = (TextMessage) message;
                String messageText = txtMsg.getText();
                LOGGER.log(Level.TRACE, "Server(" + serverId + ") received TextMessage[" + messageText + "]");
                String[] temp = messageText.split("from");
                String intValue = temp[0].replaceAll("[^0-9]+", "");
                int msgCount = Integer.parseInt(intValue);

                if ((serverId % 2 == 0 && msgCount % 2 == 0) || (serverId % 2 != 0 && msgCount % 2 != 0)) {
                    LOGGER.log(Level.INFO, "Server(" + serverId + ") take the msg and send ACK");
                    TextMessage response = this.session.createTextMessage();
                    response.setText("Server(" + serverId + ") got messageId: " + message.getJMSCorrelationID());
                    response.setJMSCorrelationID(message.getJMSCorrelationID());
                    this.replyProducer.send(message.getJMSReplyTo(), response);
                
                    sleep(500);
                    response = this.session.createTextMessage();
                    response.setText("Server(" + serverId + ") process messageId: " + message.getJMSCorrelationID());
                    response.setJMSCorrelationID(message.getJMSCorrelationID());
                    this.replyProducer.send(message.getJMSReplyTo(), response);
                }
                LOGGER.log(Level.INFO, "Press x + <Enter> to terminate the Server  \n");

            }

        } catch (JMSException e) {
            LOGGER.log(Level.ERROR, e);
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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

        Boolean myStartBroker = true;
        int myServerId = 1;
        Server myServer = null;

        LOGGER.log(Level.TRACE, "Anzahl Parameter: " + args.length);

        if (args.length > 0) {
            if ("?".equals(args[0])) {
                LOGGER.log(Level.INFO, "java -jar Server serverId, [startBroker=true|false]");
            } else {
                myServerId = Integer.parseInt(args[0]);
            }

            if (args.length == 2 && Boolean.parseBoolean(args[1]) == false) {
                myStartBroker = false;
            }
            if (myStartBroker) {
                LOGGER.log(Level.TRACE, "Start Broker");
                Server.startBroker();
            }

        }

        myServer = new Server(myServerId);

    }
}

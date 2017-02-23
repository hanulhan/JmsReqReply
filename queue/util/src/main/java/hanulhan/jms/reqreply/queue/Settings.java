package hanulhan.jms.reqreply.queue;


import javax.jms.Session;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author uhansen
 */
public class Settings {
//    public static final String MESSAGE_BROKER_URL = "tcp://localhost:61616";
//    public static final String MESSAGE_BROKER_URL="tcp://192.168.1.61:61616";
    public static final String MESSAGE_BROKER_URL = "tcp://192.168.21.10:61616";
    public static final String MESSAGE_QUEUE_NAME = "client.message.reqreply";
    public static final int SERVER_ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    public static final int CLIENT_ACK_MODE = Session.AUTO_ACKNOWLEDGE;
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hanulhan.jms.reqreply.textmessage.util;

import javax.jms.Session;

/**
 *
 * @author uhansen
 */
public class Settings {
    public static final Boolean BROKER_START_FLAG=true;
    public static final String MESSAGE_BROKER_URL="tcp://localhost:61616";
    public static final String MESSAGE_QUEUE_NAME="client.message";
    public static final int SERVER_ACK_MODE=Session.AUTO_ACKNOWLEDGE;
    public static final int CLIENT_ACK_MODE=Session.AUTO_ACKNOWLEDGE;
    
}

package hanulhan.jms.reqreply.topic;

import java.util.ArrayList;
import java.util.List;
import javax.jms.DeliveryMode;
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
    public static final String MESSAGE_TOPIC_NAME = "vmReqTopic";

    // Server
    public static final int REQ_ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    public static final int REQ_DELIVERY_MODE = DeliveryMode.PERSISTENT;
    // Client
    public static final int REP_ACK_MODE = Session.AUTO_ACKNOWLEDGE;
    public static final int REP_DELIVIRY_MODE = DeliveryMode.NON_PERSISTENT;
    public static final String[] idents = {"AAAA", "BBBB", "CCCC", "DDDD", "EEEE", "FFFF"};

    public static final String PROPERTY_NAME_COUNT = "count";
    public static final String PROPERTY_NAME_TOTAL_COUNT = "toTalcount";
    public static final String PROPERTY_NAME_IDENT = "systemIdent";
    
    public static List<String> getIdentList(int serverId) {
        List<String> myList = new ArrayList<>();
        
        for (int i = 0; i < idents.length; i++) {
            if (serverId % 2 == 0 && i % 2 == 0)    {
                myList.add(idents[i]);
            } else if (serverId % 2 != 0 && i % 2 != 0)    {
                myList.add(idents[i]);
            }
        }
        
        return myList;

    }
}

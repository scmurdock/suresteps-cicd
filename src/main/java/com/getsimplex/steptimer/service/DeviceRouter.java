package com.getsimplex.steptimer.service;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.getsimplex.steptimer.com.getsimplex.steptimer.sensormessages.SensorDeserializer;
import com.getsimplex.steptimer.com.getsimplex.steptimer.sensormessages.SensorMessage;
import com.getsimplex.steptimer.model.DeviceInterest;
import com.getsimplex.steptimer.model.DeviceInterestEnded;
import com.getsimplex.steptimer.model.DeviceMessage;
import org.eclipse.jetty.websocket.api.Session;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by sean on 8/16/2016.
 */
public class DeviceRouter extends UntypedActor {
    private static Logger logger = Logger.getLogger(DeviceRouter.class.getName());
    private static HashMap<String, Session> deviceRegistry = new HashMap<String, Session>();
    private static HashMap<String,ActorRef> uniqueDeviceListeners = new HashMap<String, ActorRef>();
    private long lastMessageDate = 0l;	

    public void onReceive(Object object){
        if (object instanceof DeviceMessage) {
            DeviceMessage deviceMessage = (DeviceMessage) object;		
            logger.info("DeviceRouter received payload: "+deviceMessage.getMessage()+" with timestamp: "+deviceMessage.getDate());
            try {
                if ((deviceMessage.getDate()-lastMessageDate>600) && deviceRegistry.containsKey(deviceMessage.getDeviceId())){//cancel out noise -- usually extra steps that aren't real happen within 500 milliseconds, but longer than that and it's real
                    //getDeviceListener(deviceMessage.getDeviceId()).tell(object,self());
                    deviceRegistry.get(deviceMessage.getDeviceId()).getRemote().sendString(deviceMessage.getMessage());
                }
            } catch (Exception e){
                logger.severe("Unable to unmarshal message: "+deviceMessage.getMessage());
            }
	    lastMessageDate = deviceMessage.getDate();//update it	

        } else if (object instanceof DeviceInterest){
            DeviceInterest deviceInterest = (DeviceInterest) object;
            if(!deviceRegistry.containsKey(deviceInterest.getDeviceId())){
                deviceRegistry.put(deviceInterest.getDeviceId(),deviceInterest.getInterestedSession());
            } else {
                logger.info("Device: "+deviceInterest.getDeviceId()+" is already being monitored.");
            }
        } else if (object instanceof DeviceInterestEnded){
            DeviceInterestEnded deviceInterestEnded = (DeviceInterestEnded) object;
            if (deviceRegistry.containsKey(deviceInterestEnded.getDeviceId())){
                deviceRegistry.remove(deviceInterestEnded.getDeviceId());
            } else if (deviceRegistry.containsValue(deviceInterestEnded.getInterestedSession())){
                removeRegistryValue(deviceInterestEnded.getInterestedSession());
            }
        }


    }

    private void removeRegistryValue(Session session){
        Set<Map.Entry<String,Session>> registryEntries = deviceRegistry.entrySet();

        for (Map.Entry<String,Session> registryEntry:registryEntries){
            if (session.equals(registryEntry.getValue())){
                deviceRegistry.remove(registryEntry.getKey());
            }
        }
    }

    ActorRef getDeviceListener(String deviceId){
        ActorRef deviceListener;
        if(uniqueDeviceListeners.containsKey(deviceId)){
            deviceListener = uniqueDeviceListeners.get(deviceId);
        } else {
            deviceListener = context().actorOf(Props.create(Device.class), deviceId);
            uniqueDeviceListeners.put(deviceId, deviceListener);
        }

        return deviceListener;
    }
}

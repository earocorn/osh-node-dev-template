package com.sample.impl.sensor.wii;

import motej.Mote;
import motej.event.AccelerometerEvent;
import motej.event.AccelerometerListener;
import motej.event.CoreButtonEvent;
import motej.event.CoreButtonListener;
import org.checkerframework.checker.signature.qual.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class WiiObserver implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(WiiObserver.class);
    public static WiiObserver wiiObserver;
    private Thread worker;
    private final Object processingLock = new Object();
    private Map<Identifier, AccelerometerListener> wiiRemoteListeners;

    public static synchronized  WiiObserver getInstance(){
        if(wiiObserver ==null){
            wiiObserver = new WiiObserver();
        }
        return wiiObserver;
    }
    private WiiObserver(){



    }
//
//    public void doStart(){
//        worker = new Thread(this);
//
//        logger.info("Starting worker thread: {}", worker.getName());
//
//        worker.start();
//    }
//    public void doStop(){
//        synchronized (processingLock) {
//
//            stopProcessing = true;
//        }
//    }
//
//    public void getButtonData(Mote mote){
//        mote.addCoreButtonListener(new CoreButtonListener() {
//            @Override
//            public void buttonPressed(CoreButtonEvent coreButtonEvent) {
//                BUTTON_A = coreButtonEvent.isButtonAPressed();
//                BUTTON_B = coreButtonEvent.isButtonBPressed();
//                BUTTON_MINUS = coreButtonEvent.isButtonMinusPressed();
//                BUTTON_PLUS = coreButtonEvent.isButtonPlusPressed();
//                BUTTON_HOME = coreButtonEvent.isButtonHomePressed();
//                D_PAD_LEFT = coreButtonEvent.isDPadLeftPressed();
//                D_PAD_RIGHT = coreButtonEvent.isDPadRightPressed();
//                D_PAD_DOWN = coreButtonEvent.isDPadDownPressed();
//                D_PAD_UP = coreButtonEvent.isDPadUpPressed();
//                NONE = coreButtonEvent.isNoButtonPressed();
//                BUTTON_TWO = coreButtonEvent.isButtonTwoPressed();
//                BUTTON_ONE = coreButtonEvent.isButtonOnePressed();
//
//            }
//        });
//
//    }
//
//    public void getAccelData(Mote mote) {
//        mote.addAccelerometerListener(new AccelerometerListener<Mote>() {
//            @Override
//            public void accelerometerChanged(AccelerometerEvent accelerometerEvent) {
//                motionX = accelerometerEvent.getX();
//                motionY = accelerometerEvent.getY();
//                motionZ = accelerometerEvent.getZ();
//            }
//        });
//
//    }
//
    @Override
    public void run() {



    }
}

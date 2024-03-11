package com.sample.impl.sensor.wii;


import javafx.util.Pair;
import motej.Mote;
import motej.MoteFinder;
import motej.MoteFinderListener;

import java.util.ArrayList;

public class PairWii {
    private static PairWii instance;
    ArrayList<Mote> motes= new ArrayList<>();
    Mote mote;
    boolean[] remoteLEDs = new boolean[4];

    public static synchronized PairWii getInstance(){
        if(instance == null){
            instance = new PairWii();
        }
        return instance;
    }
    private PairWii(){
        remoteLEDs[0] = false;
        remoteLEDs[1] = true;
        remoteLEDs[2] = false;
        remoteLEDs[3] = false;
    }
    public Mote findMote(){
        MoteFinderListener listener = new MoteFinderListener() {
            @Override
            public void moteFound(Mote mote) {
                System.out.println("mote found: "+ mote.getBluetoothAddress());
                mote.rumble(2000L);
                mote.setPlayerLeds(remoteLEDs);
                motes.add(mote);
                PairWii.this.mote = mote;
            }
        };
        MoteFinder finder = MoteFinder.getMoteFinder();
        finder.addMoteFinderListener(listener);

        System.out.println("starting discovery");
        finder.startDiscovery();

        System.out.println("Putting thread to sleep..");
        try {
            Thread.sleep(30000L);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Stopping discovery..");
        finder.stopDiscovery();

        return mote;
    }


}
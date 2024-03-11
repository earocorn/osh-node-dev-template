//package com.sample.impl.sensor.wii;
//
//
//import motej.event.*;
//import motej.Mote;
//import motej.request.ReportModeRequest;
//
//
//public class WiiRemote {
//    private static WiiRemote instance;
//    private Mote mote;
//    private final int[] motion = {0, 0, 0};
////    private final Map<Button, Boolean> buttonStates = new HashMap<>();
//    public enum Button {NONE, BUTTON_A, BUTTON_B, BUTTON_HOME, BUTTON_MINUS, BUTTON_ONE, BUTTON_PLUS, BUTTON_TWO, D_PAD_DOWN, D_PAD_LEFT, D_PAD_RIGHT, D_PAD_UP}
//
//    public static WiiRemote getInstance() {
//        if (instance == null) {
//            instance = new WiiRemote();
//        }
//        return instance;
//    }
//
//    private WiiRemote() {
//        mote = PairWii.getInstance().mote;
//
//        if (mote != null) {
//            System.out.println("getting button and accel data");
//            getAccelData(mote);
//            getButtonData(mote);
//            mote.setReportMode(ReportModeRequest.DATA_REPORT_0x31);
//            mote.setReportMode(ReportModeRequest.DATA_REPORT_0x30);
//        }
//
////        if(mote != null){
////            mote.addCoreButtonListener(new CoreButtonListener() {
////                @Override
////                public void buttonPressed(CoreButtonEvent coreButtonEvent) {
////                    handleButtonPressed(coreButtonEvent);
////                }
////            });
////            mote.addAccelerometerListener(new AccelerometerListener<Mote>() {
////                @Override
////                public void accelerometerChanged(AccelerometerEvent<Mote> accelerometerEvent) {
////                    motion[0] = accelerometerEvent.getX();
////                    motion[1] = accelerometerEvent.getY();
////                    motion[2] = accelerometerEvent.getZ();
////
////                }
////            });
//
////        }
//
//    }
//
//    public void getButtonData(Mote mote) {
//        mote.addCoreButtonListener(new CoreButtonListener() {
//            @Override
//            public void buttonPressed(CoreButtonEvent coreButtonEvent) {
//                if (coreButtonEvent.isButtonAPressed()) {
//                    System.out.println("BUTTON_A");
//                    handleButtonPressed(Button.BUTTON_A);
//                } else if (coreButtonEvent.isButtonBPressed()) {
//                    System.out.println("BUTTON_B");
//                } else if (coreButtonEvent.isButtonPlusPressed()) {
//                    System.out.println("BUTTON_PLUS");
//                } else if (coreButtonEvent.isButtonMinusPressed()) {
//                    System.out.println("BUTTON_MINUS");
//                } else if (coreButtonEvent.isButtonHomePressed()) {
//                    System.out.println("BUTTON_HOME");
//                } else if (coreButtonEvent.isDPadLeftPressed()) {
//                    System.out.println("D_PAD_LEFT");
//                } else if (coreButtonEvent.isDPadRightPressed()) {
//                    System.out.println("D_PAD_RIGHT");
//                } else if (coreButtonEvent.isDPadUpPressed()) {
//                    System.out.println("D_PAD_UP");
//                } else if (coreButtonEvent.isDPadDownPressed()) {
//                    System.out.println("D_PAD_DOWN");
//                } else if (coreButtonEvent.isButtonOnePressed()) {
//                    System.out.println("BUTTON_ONE");
//                } else if (coreButtonEvent.isButtonTwoPressed()) {
//                    System.out.println("BUTTON_TWO");
//                } else if (coreButtonEvent.isNoButtonPressed()) {
//                    System.out.println("NONE");
//                }
//            }
//        });
//    }
//
//    public void getAccelData(Mote mote) {
//        mote.addAccelerometerListener(new AccelerometerListener<Mote>() {
//            @Override
//            public void accelerometerChanged(AccelerometerEvent<Mote> accelerometerEvent) {
//                motion[0] = accelerometerEvent.getX();
//                motion[1] = accelerometerEvent.getY();
//                motion[2] = accelerometerEvent.getZ();
//            }
//        });
//
//    }
//
//
//    public void handleButtonPressed(Button button) {
//        boolean pressed = false;
//        switch (button) {
//            case D_PAD_UP:
//                System.out.println("d-pad-up trueeee");
//
//                break;
//
//            case D_PAD_DOWN:
//                System.out.println("d-pad-down");
//                break;
//
//            case D_PAD_RIGHT:
//                System.out.println("d-pad-right");
//                break;
//
//            case D_PAD_LEFT:
//                System.out.println("d-pad-left");
//                break;
//
//            case NONE:
//                System.out.println("none");
//                break;
//
//            case BUTTON_A:
//                System.out.println("a");
//                break;
//
//            case BUTTON_B:
//                System.out.println("b");
//                break;
//
//            case BUTTON_ONE:
//                System.out.println("1");
//                break;
//
//            case BUTTON_HOME:
//                System.out.println("home");
//                break;
//
//            case BUTTON_TWO:
//                System.out.println("2");
//                break;
//
//            case BUTTON_MINUS:
//                System.out.println("minus");
//                break;
//
//        }
//    }
//
////    public boolean getButton(Button button) {
////
//////                CoreButtonEvent event = new CoreButtonEvent(mote, 0);
//////        CoreButtonEvent event;
////        switch (button) {
////            case D_PAD_UP:
////                return true;
//////                event = new CoreButtonEvent(mote, 8);
//////                return event.isDPadUpPressed();
////            case D_PAD_DOWN:
////                return true;
//////                event = new CoreButtonEvent(mote, 4);
//////                return event.isDPadDownPressed();
////            case D_PAD_RIGHT:
////                return true;
//////                event = new CoreButtonEvent(mote, 2);
//////                return event.isDPadRightPressed();
////            case D_PAD_LEFT:
//////                event = new CoreButtonEvent(mote, 1);
//////                return event.isDPadLeftPressed();
////            case NONE:
//////                event = new CoreButtonEvent(mote, 0);
//////                return event.isNoButtonPressed();
////            case BUTTON_A:
//////                event = new CoreButtonEvent(mote, 2048);
//////                return event.isButtonAPressed();
////            case BUTTON_B:
//////                event = new CoreButtonEvent(mote, 1024);
//////                return event.isButtonBPressed();
////            case BUTTON_ONE:
//////                event = new CoreButtonEvent(mote, 512);
//////                return event.isButtonOnePressed();
////            case BUTTON_HOME:
//////                event = new CoreButtonEvent(mote, 32768);
//////                return event.isButtonHomePressed();
////            case BUTTON_TWO:
//////                event = new CoreButtonEvent(mote, 256);
//////                return event.isButtonTwoPressed();
////            case BUTTON_PLUS:
//////                event = new CoreButtonEvent(mote, 16);
//////                return event.isButtonPlusPressed();
////            case BUTTON_MINUS:
//////                event = new CoreButtonEvent(mote, 4096);
//////                return event.isButtonMinusPressed();
////            default:
////                return false;
////        }
////    }
//
//        //        if (coreButtonEvent.isButtonAPressed()) {
////            System.out.println("BUTTON_A");
////        } else if (coreButtonEvent.isButtonBPressed()) {
////            System.out.println("BUTTON_B");
////        } else if (coreButtonEvent.isButtonPlusPressed()) {
////            System.out.println("BUTTON_PLUS");
////        } else if (coreButtonEvent.isButtonMinusPressed()) {
////            System.out.println("BUTTON_MINUS");
////        } else if (coreButtonEvent.isButtonHomePressed()) {
////            System.out.println("BUTTON_HOME");
////        } else if (coreButtonEvent.isDPadLeftPressed()) {
////            System.out.println("D_PAD_LEFT");
////        } else if (coreButtonEvent.isDPadRightPressed()) {
////            System.out.println("D_PAD_RIGHT");
////        } else if (coreButtonEvent.isDPadUpPressed()) {
////            System.out.println("D_PAD_UP");
////        } else if (coreButtonEvent.isDPadDownPressed()) {
////            System.out.println("D_PAD_DOWN");
////        } else if (coreButtonEvent.isButtonOnePressed()) {
////            System.out.println("BUTTON_ONE");
////        } else if (coreButtonEvent.isButtonTwoPressed()) {
////            System.out.println("BUTTON_TWO");
////        } else if (coreButtonEvent.isNoButtonPressed()) {
////            System.out.println("NONE");
////        }
//
////    private void updateButtonState(Button button, boolean state){
////        buttonStates.put(button,state);
////    }
////
////    public boolean isButtonPressed(Button button){
////        return buttonStates.getOrDefault(button,false);
////    }
//
////    updateButtonState(Button.D_PAD_DOWN, coreButtonEvent.isDPadDownPressed());
////                    updateButtonState(Button.D_PAD_RIGHT, coreButtonEvent.isDPadRightPressed());
////                    updateButtonState(Button.D_PAD_LEFT, coreButtonEvent.isDPadLeftPressed());
////                    updateButtonState(Button.D_PAD_UP, coreButtonEvent.isDPadUpPressed());
////                    updateButtonState(Button.BUTTON_MINUS, coreButtonEvent.isButtonMinusPressed());
////                    updateButtonState(Button.BUTTON_B, coreButtonEvent.isButtonBPressed());
////                    updateButtonState(Button.BUTTON_A, coreButtonEvent.isButtonAPressed());
////                    updateButtonState(Button.BUTTON_PLUS, coreButtonEvent.isButtonPlusPressed());
////                    updateButtonState(Button.BUTTON_ONE, coreButtonEvent.isButtonOnePressed());
////                    updateButtonState(Button.BUTTON_TWO, coreButtonEvent.isButtonTwoPressed());
////                    updateButtonState(Button.BUTTON_HOME, coreButtonEvent.isButtonHomePressed());
////                    updateButtonState(Button.NONE, coreButtonEvent.isNoButtonPressed());
//
//
//}
//
//

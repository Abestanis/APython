package com.apython.python.pythonhost.views.sdl;

import android.content.Context;
import android.os.Build;
import android.os.Vibrator;
import android.view.InputDevice;

import java.util.ArrayList;

class SDLHapticHandler {

    class SDLHaptic {
        int      device_id;
        String   name;
        Vibrator vib;
    }

    private final ArrayList<SDLHaptic> hapticDevices;
    private final SDLServer      sdlServer;

    SDLHapticHandler(SDLServer sdlServer) {
        this.sdlServer = sdlServer;
        hapticDevices = new ArrayList<>();
    }

    public void run(int device_id, int length) {
        SDLHaptic haptic = getHaptic(device_id);
        if (haptic != null) {
            haptic.vib.vibrate(length);
        }
    }

    void pollHapticDevices() {
        final int deviceId_VIBRATOR_SERVICE = 999999;
        boolean hasVibratorService = false;

        int[] deviceIds = InputDevice.getDeviceIds();
        // It helps processing the device ids in reverse order
        // For example, in the case of the XBox 360 wireless dongle,
        // so the first controller seen by SDL matches what the receiver
        // considers to be the first controller

        if (Build.VERSION.SDK_INT >= 16)
        {
            for (int i = deviceIds.length - 1; i > -1; i--) {
                SDLHaptic haptic = getHaptic(deviceIds[i]);
                if (haptic == null) {
                    InputDevice device = InputDevice.getDevice(deviceIds[i]);
                    Vibrator vib = device.getVibrator();
                    if (vib.hasVibrator()) {
                        haptic = new SDLHaptic();
                        haptic.device_id = deviceIds[i];
                        haptic.name = device.getName();
                        haptic.vib = vib;
                        hapticDevices.add(haptic);
                        SDLControllerManager.nativeAddHaptic(haptic.device_id, haptic.name);
                    }
                }
            }
        }

        /* Check VIBRATOR_SERVICE */
        Vibrator vib = (Vibrator) sdlServer.getActivity()
                .getSystemService(Context.VIBRATOR_SERVICE);
        if (vib != null) {
            hasVibratorService = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                    || vib.hasVibrator();
            if (hasVibratorService) {
                SDLHaptic haptic = getHaptic(deviceId_VIBRATOR_SERVICE);
                if (haptic == null) {
                    haptic = new SDLHaptic();
                    haptic.device_id = deviceId_VIBRATOR_SERVICE;
                    haptic.name = "VIBRATOR_SERVICE";
                    haptic.vib = vib;
                    hapticDevices.add(haptic);
                    SDLControllerManager.nativeAddHaptic(haptic.device_id, haptic.name);
                }
            }
        }

        /* Check removed devices */
        ArrayList<Integer> removedDevices = new ArrayList<Integer>();
        for(int i = 0; i < hapticDevices.size(); i++) {
            int device_id = hapticDevices.get(i).device_id;
            int j;
            for (j=0; j < deviceIds.length; j++) {
                if (device_id == deviceIds[j]) break;
            }

            if (device_id == deviceId_VIBRATOR_SERVICE && hasVibratorService) {
                // don't remove the vibrator if it is still present
            } else if (j == deviceIds.length) {
                removedDevices.add(device_id);
            }
        }

        for(int i=0; i < removedDevices.size(); i++) {
            int device_id = removedDevices.get(i);
            SDLControllerManager.nativeRemoveHaptic(device_id);
            for (int j = 0; j < hapticDevices.size(); j++) {
                if (hapticDevices.get(j).device_id == device_id) {
                    hapticDevices.remove(j);
                    break;
                }
            }
        }
    }

    private SDLHaptic getHaptic(int device_id) {
        for (SDLHaptic haptic : hapticDevices) {
            if (haptic.device_id == device_id) {
                return haptic;
            }
        }
        return null;
    }
}

package com.apython.python.pythonhost.views.sdl;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.InputDevice;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A joystick handler for SDL. 
 * 
 * Created by Sebastian on 21.11.2015.
 */
@SuppressLint("NewApi")
class SDLJoystickHandler {
    private class SDLJoystick {
        int                                device_id;
        String                             name;
        ArrayList<InputDevice.MotionRange> axes;
        ArrayList<InputDevice.MotionRange> hats;
    }

    private class RangeComparator implements Comparator<InputDevice.MotionRange> {
        @Override
        public int compare(InputDevice.MotionRange arg0, InputDevice.MotionRange arg1) {
            return arg0.getAxis() - arg1.getAxis();
        }
    }

    private final ArrayList<SDLJoystick> joysticks;
    private final boolean                isApiAvailable;

    SDLJoystickHandler() {
        isApiAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
        joysticks = new ArrayList<>();
    }

    // Check if a given device is considered a possible SDL joystick
    static boolean isDeviceSDLJoystick(int deviceId) {
        final int SOURCE_GAMEPAD = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
                ? InputDevice.SOURCE_GAMEPAD : 0x00000401;
        InputDevice device = InputDevice.getDevice(deviceId);
        // We cannot use InputDevice.isVirtual before API 16, so let's accept
        // only non-negative device ids (VIRTUAL_KEYBOARD equals -1)
        if ((device == null) || (deviceId < 0)) {
            return false;
        }
        int sources = device.getSources();
        return (((sources & InputDevice.SOURCE_CLASS_JOYSTICK) == InputDevice.SOURCE_CLASS_JOYSTICK) ||
                ((sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) ||
                ((sources & SOURCE_GAMEPAD) == SOURCE_GAMEPAD)
        );
    }
    
    void pollInputDevices() {
        if (!isApiAvailable) { return; }
        int[] deviceIds = InputDevice.getDeviceIds();
        // It helps processing the device ids in reverse order
        // For example, in the case of the XBox 360 wireless dongle,
        // so the first controller seen by SDL matches what the receiver
        // considers to be the first controller

        for (int i = deviceIds.length - 1; i > -1; i--) {
            SDLJoystick joystick = getJoystick(deviceIds[i]);
            if (joystick == null) {
                joystick = new SDLJoystick();
                InputDevice joystickDevice = InputDevice.getDevice(deviceIds[i]);
                if (isDeviceSDLJoystick(deviceIds[i])) {
                    joystick.device_id = deviceIds[i];
                    joystick.name = joystickDevice.getName();
                    joystick.axes = new ArrayList<>();
                    joystick.hats = new ArrayList<>();

                    List<InputDevice.MotionRange> ranges = joystickDevice.getMotionRanges();
                    Collections.sort(ranges, new RangeComparator());
                    for (InputDevice.MotionRange range : ranges ) {
                        if ((range.getSource() & InputDevice.SOURCE_CLASS_JOYSTICK) != 0) {
                            if (range.getAxis() == MotionEvent.AXIS_HAT_X ||
                                    range.getAxis() == MotionEvent.AXIS_HAT_Y) {
                                joystick.hats.add(range);
                            }
                            else {
                                joystick.axes.add(range);
                            }
                        }
                    }

                    joysticks.add(joystick);
                    SDLWindowFragment.nativeAddJoystick(joystick.device_id, joystick.name, 0, -1,
                                                        joystick.axes.size(), joystick.hats.size() / 2, 0);
                }
            }
        }

        /* Check removed devices */
        ArrayList<Integer> removedDevices = new ArrayList<>();
        for(int i=0; i < joysticks.size(); i++) {
            int device_id = joysticks.get(i).device_id;
            int j;
            for (j=0; j < deviceIds.length; j++) {
                if (device_id == deviceIds[j]) break;
            }
            if (j == deviceIds.length) {
                removedDevices.add(device_id);
            }
        }

        for(int i=0; i < removedDevices.size(); i++) {
            int device_id = removedDevices.get(i);
            SDLWindowFragment.nativeRemoveJoystick(device_id);
            for (int j=0; j < joysticks.size(); j++) {
                if (joysticks.get(j).device_id == device_id) {
                    joysticks.remove(j);
                    break;
                }
            }
        }
    }

    private SDLJoystick getJoystick(int device_id) {
        for(int i=0; i < joysticks.size(); i++) {
            if (joysticks.get(i).device_id == device_id) {
                return joysticks.get(i);
            }
        }
        return null;
    }

    @SuppressLint("NewApi")
    boolean handleMotionEvent(MotionEvent event) {
        if (!isApiAvailable) { return false; }
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) != 0) {
            int actionPointerIndex = event.getActionIndex();
            int action = event.getActionMasked();
            switch(action) {
            case MotionEvent.ACTION_MOVE:
                SDLJoystick joystick = getJoystick(event.getDeviceId());
                if (joystick != null) {
                    for (int i = 0; i < joystick.axes.size(); i++) {
                        InputDevice.MotionRange range = joystick.axes.get(i);
                        /* Normalize the value to -1...1 */
                        float value = (event.getAxisValue(range.getAxis(), actionPointerIndex) - range.getMin())
                                / range.getRange() * 2.0f - 1.0f;
                        SDLWindowFragment.onNativeJoy(joystick.device_id, i, value);
                    }
                    for (int i = 0; i < joystick.hats.size(); i += 2) {
                        int hatX = Math.round(event.getAxisValue(joystick.hats.get(i).getAxis(), actionPointerIndex));
                        int hatY = Math.round(event.getAxisValue(joystick.hats.get(i + 1).getAxis(), actionPointerIndex));
                        SDLWindowFragment.onNativeHat(joystick.device_id, i / 2, hatX, hatY);
                    }
                }
                break;
            default:
                break;
            }
        }
        return true;
    }
}

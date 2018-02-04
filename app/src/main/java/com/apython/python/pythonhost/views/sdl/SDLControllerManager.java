package com.apython.python.pythonhost.views.sdl;

import android.view.InputDevice;

import com.apython.python.pythonhost.CalledByNative;

import java.util.Arrays;


public class SDLControllerManager {
    private final SDLJoystickHandler joystickHandler;
    private final SDLHapticHandler   hapticHandler;

    public SDLControllerManager(SDLServer sdlServer) {
        joystickHandler = new SDLJoystickHandler();
        hapticHandler = new SDLHapticHandler(sdlServer);
    }
    
    SDLJoystickHandler getJoystickHandler() {
        return joystickHandler;
    }

    @CalledByNative
    public void pollInputDevices() {
        joystickHandler.pollInputDevices();
    }

    @CalledByNative
    public void pollHapticDevices() {
        hapticHandler.pollHapticDevices();
    }

    @CalledByNative
    public void hapticRun(int device_id, int length) {
        hapticHandler.run(device_id, length);
    }
    
    /**
     * @return An array which may be empty but is never null.
     */
    @CalledByNative
    public static int[] getInputDeviceIds(int sources) {
        int[] ids = InputDevice.getDeviceIds();
        int[] filtered = new int[ids.length];
        int used = 0;
        for (int id : ids) {
            InputDevice device = InputDevice.getDevice(id);
            if ((device != null) && ((device.getSources() & sources) != 0)) {
                filtered[used++] = device.getId();
            }
        }
        return Arrays.copyOf(filtered, used);
    }

    public static native int onNativePadDown(int device_id, int keycode);
    public static native int onNativePadUp(int device_id, int keycode);
    public static native void onNativeJoy(int device_id, int axis,
                                          float value);
    public static native void onNativeHat(int device_id, int hat_id,
                                          int x, int y);
    public static native int nativeAddJoystick(int device_id, String name, String desc,
                                               int is_accelerometer, int nbuttons,
                                               int naxes, int nhats, int nballs);
    public static native int nativeRemoveJoystick(int device_id);
    public static native int nativeAddHaptic(int device_id, String name);
    public static native int nativeRemoveHaptic(int device_id);
}

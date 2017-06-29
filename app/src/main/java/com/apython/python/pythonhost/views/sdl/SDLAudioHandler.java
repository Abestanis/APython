package com.apython.python.pythonhost.views.sdl;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

/**
 * An Audio handler that enables sdl applications to play audio on Android devices.
 * 
 * Created by Sebastian on 21.11.2015.
 */
class SDLAudioHandler {
    private static final String TAG = "SDL_Audio";

    private AudioTrack audioTrack = null;
    private static AudioRecord audioRecord = null;

    int audioOpen(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);

        Log.v(TAG, "SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " +
                (is16Bit ? "16-bit" : "8-bit") + " " + (sampleRate / 1000f) + "kHz, " + 
                desiredFrames + " frames buffer");

        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        int minBuffSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        desiredFrames = Math.max(desiredFrames, (minBuffSize + frameSize - 1) / frameSize);

        if (audioTrack == null) {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                                        audioFormat, desiredFrames * frameSize,
                                        AudioTrack.MODE_STREAM);

            // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
            // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
            // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()

            if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "Failed during initialization of Audio Track");
                audioTrack = null;
                return -1;
            }

            audioTrack.play();
        }

        Log.v(TAG, "SDL audio: got " + ((audioTrack.getChannelCount() >= 2) ? "stereo" : "mono")
                + " " + ((audioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit")
                + " " + (audioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");

        return 0;
    }

    int captureOpen(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo 
                ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);

        Log.v(TAG, "SDL capture: wanted " + (isStereo ? "stereo" : "mono") + " " +
                (is16Bit ? "16-bit" : "8-bit") + " " +
                (sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");

        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        int minBuffSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
        desiredFrames = Math.max(desiredFrames, (minBuffSize + frameSize - 1) / frameSize);

        if (audioRecord == null) {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate,
                                           channelConfig, audioFormat, desiredFrames * frameSize);

            // see notes about AudioTrack state in audioOpen(), above. Probably also applies here.
            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed during initialization of AudioRecord");
                audioRecord.release();
                audioRecord = null;
                return -1;
            }

            audioRecord.startRecording();
        }

        Log.v(TAG, "SDL capture: got " + ((audioRecord.getChannelCount() >= 2) ? "stereo" : "mono") + " " +
                ((audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " +
                (audioRecord.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");

        return 0;
    }

    void audioClose() {
        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    void captureClose() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    void audioWriteShortBuffer(short[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = audioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            } else {
                Log.w(TAG, "SDL audio: error return from write(short)");
                return;
            }
        }
    }

    void audioWriteByteBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = audioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {}
            } else {
                Log.w(TAG, "SDL audio: error return from write(byte)");
                return;
            }
        }
    }
    
    int captureReadShortBuffer(short[] buffer, boolean blocking) {
        // !!! FIXME: this is available in API Level 23. Until then, we always block.  :(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return audioRecord.read(buffer, 0, buffer.length, blocking
                    ? AudioRecord.READ_BLOCKING : AudioRecord.READ_NON_BLOCKING);
        }
        return audioRecord.read(buffer, 0, buffer.length);
    }

    int captureReadByteBuffer(byte[] buffer, boolean blocking) {
        // !!! FIXME: this is available in API Level 23. Until then, we always block.  :(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return audioRecord.read(buffer, 0, buffer.length, blocking
                    ? AudioRecord.READ_BLOCKING : AudioRecord.READ_NON_BLOCKING);
        }
        return audioRecord.read(buffer, 0, buffer.length);
    }
}

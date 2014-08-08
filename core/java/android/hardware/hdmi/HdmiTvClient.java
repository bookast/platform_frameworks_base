/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.hardware.hdmi;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.hardware.hdmi.HdmiRecordSources.RecordSource;
import android.hardware.hdmi.HdmiTimerRecordSources.TimerRecordSource;
import android.os.RemoteException;
import android.util.Log;

import libcore.util.EmptyArray;

/**
 * HdmiTvClient represents HDMI-CEC logical device of type TV in the Android system
 * which acts as TV/Display. It provides with methods that manage, interact with other
 * devices on the CEC bus.
 *
 * @hide
 */
@SystemApi
public final class HdmiTvClient extends HdmiClient {
    private static final String TAG = "HdmiTvClient";

    HdmiTvClient(IHdmiControlService service) {
        super(service);
    }

    // Factory method for HdmiTvClient.
    // Declared package-private. Accessed by HdmiControlManager only.
    static HdmiTvClient create(IHdmiControlService service) {
        return new HdmiTvClient(service);
    }

    @Override
    public int getDeviceType() {
        return HdmiDeviceInfo.DEVICE_TV;
    }

    /**
     * Callback interface used to get the result of {@link #deviceSelect}.
     */
    public interface SelectCallback {
        /**
         * Called when the operation is finished.
         *
         * @param result the result value of {@link #deviceSelect}
         */
        void onComplete(int result);
    }

    /**
     * Select a CEC logical device to be a new active source.
     *
     * @param logicalAddress logical address of the device to select
     * @param callback callback to get the result with
     * @throws {@link IllegalArgumentException} if the {@code callback} is null
     */
    public void deviceSelect(int logicalAddress, @NonNull SelectCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null.");
        }
        try {
            mService.deviceSelect(logicalAddress, getCallbackWrapper(callback));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to select device: ", e);
        }
    }

    /**
     * Select a HDMI port to be a new route path.
     *
     * @param portId HDMI port to select
     * @param callback callback to get the result with
     * @throws {@link IllegalArgumentException} if the {@code callback} is null
     */
    public void portSelect(int portId, @NonNull SelectCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Callback must not be null");
        }
        try {
            mService.portSelect(portId, getCallbackWrapper(callback));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to select port: ", e);
        }
    }

    /**
     * Set system audio volume
     *
     * @param oldIndex current volume index
     * @param newIndex volume index to be set
     * @param maxIndex maximum volume index
     */
    public void setSystemAudioVolume(int oldIndex, int newIndex, int maxIndex) {
        try {
            mService.setSystemAudioVolume(oldIndex, newIndex, maxIndex);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set volume: ", e);
        }
    }

    /**
     * Set system audio mute status
     *
     * @param mute {@code true} if muted; otherwise, {@code false}
     */
    public void setSystemAudioMute(boolean mute) {
        try {
            mService.setSystemAudioMute(mute);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set mute: ", e);
        }
    }

    /**
     * Set record listener
     *
     * @param listener
     */
    public void setRecordListener(@NonNull HdmiRecordListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null.");
        }
        try {
            mService.setHdmiRecordListener(getListenerWrapper(listener));
        } catch (RemoteException e) {
            Log.e(TAG, "failed to set record listener.", e);
        }
    }

    /**
     * Start one touch recording with the given recorder address and recorder source.
     * <p>
     * Usage
     * <pre>
     * HdmiTvClient tvClient = ....;
     * // for own source.
     * OwnSource ownSource = ownHdmiRecordSources.ownSource();
     * tvClient.startOneTouchRecord(recorderAddress, ownSource);
     * </pre>
     */
    public void startOneTouchRecord(int recorderAddress, @NonNull RecordSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null.");
        }

        try {
            byte[] data = new byte[source.getDataSize(true)];
            source.toByteArray(true, data, 0);
            mService.startOneTouchRecord(recorderAddress, data);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to start record: ", e);
        }
    }

    /**
     * Stop one touch record.
     *
     * @param recorderAddress recorder address where recoding will be stopped
     */
    public void stopOneTouchRecord(int recorderAddress) {
        try {
            mService.stopOneTouchRecord(recorderAddress);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to stop record: ", e);
        }
    }

    /**
     * Start timer recording with the given recoder address and recorder source.
     * <p>
     * Usage
     * <pre>
     * HdmiTvClient tvClient = ....;
     * // create timer info
     * TimerInfo timerInfo = HdmiTimerRecourdSources.timerInfoOf(...);
     * // for digital source.
     * DigitalServiceSource recordSource = HdmiRecordSources.ofDigitalService(...);
     * // create timer recording source.
     * TimerRecordSource source = HdmiTimerRecourdSources.ofDigitalSource(timerInfo, recordSource);
     * tvClient.startTimerRecording(recorderAddress, source);
     * </pre>
     *
     * @param recorderAddress target recorder address
     * @param sourceType type of record source. It should be one of
     *          {@link HdmiControlManager#TIMER_RECORDING_TYPE_DIGITAL},
     *          {@link HdmiControlManager#TIMER_RECORDING_TYPE_ANALOGUE},
     *          {@link HdmiControlManager#TIMER_RECORDING_TYPE_EXTERNAL}.
     * @param source record source to be used
     */
    public void startTimerRecording(int recorderAddress, int sourceType, TimerRecordSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null.");
        }

        checkTimerRecordingSourceType(sourceType);

        try {
            byte[] data = new byte[source.getDataSize()];
            source.toByteArray(data, 0);
            mService.startTimerRecording(recorderAddress, sourceType, data);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to start record: ", e);
        }
    }

    private void checkTimerRecordingSourceType(int sourceType) {
        switch (sourceType) {
            case HdmiControlManager.TIMER_RECORDING_TYPE_DIGITAL:
            case HdmiControlManager.TIMER_RECORDING_TYPE_ANALOGUE:
            case HdmiControlManager.TIMER_RECORDING_TYPE_EXTERNAL:
                break;
            default:
                throw new IllegalArgumentException("Invalid source type:" + sourceType);
        }
    }

    /**
     * Clear timer recording with the given recorder address and recording source.
     * For more details, please refer {@link #startTimerRecording(int, int, TimerRecordSource)}.
     */
    public void clearTimerRecording(int recorderAddress, int sourceType, TimerRecordSource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null.");
        }

        checkTimerRecordingSourceType(sourceType);
        try {
            byte[] data = new byte[source.getDataSize()];
            source.toByteArray(data, 0);
            mService.clearTimerRecording(recorderAddress, sourceType, data);
        } catch (RemoteException e) {
            Log.e(TAG, "failed to start record: ", e);
        }
    }

    private static IHdmiControlCallback getCallbackWrapper(final SelectCallback callback) {
        return new IHdmiControlCallback.Stub() {
            @Override
            public void onComplete(int result) {
                callback.onComplete(result);
            }
        };
    }

    private static IHdmiRecordListener getListenerWrapper(final HdmiRecordListener callback) {
        return new IHdmiRecordListener.Stub() {
            @Override
            public byte[] getOneTouchRecordSource(int recorderAddress) {
                HdmiRecordSources.RecordSource source =
                        callback.getOneTouchRecordSource(recorderAddress);
                if (source == null) {
                    return EmptyArray.BYTE;
                }
                byte[] data = new byte[source.getDataSize(true)];
                source.toByteArray(true, data, 0);
                return data;
            }

            @Override
            public void onOneTouchRecordResult(int result) {
                callback.onOneTouchRecordResult(result);
            }

            @Override
            public void onTimerRecordingResult(int result) {
                callback.onTimerRecordingResult(
                        HdmiRecordListener.TimerStatusData.parseFrom(result));
            }

            @Override
            public void onClearTimerRecordingResult(int result) {
                callback.onClearTimerRecordingResult(result);
            }
        };
    }
}

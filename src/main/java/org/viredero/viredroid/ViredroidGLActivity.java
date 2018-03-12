/*
 * Android app for viredero — viredroid
 * Copyright (c) 2015 Leonid Movshovich <event.riga@gmail.com>
 *
 *
 * viredero is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * viredero is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with viredero; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package org.viredero.viredroid;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.lang.StringBuilder;

import javax.microedition.khronos.egl.EGLConfig;

public class ViredroidGLActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    public static final String LOGTAG = "viredroid";

    private HeadTransform lastHeadXform;
    
    private Vibrator vibrator;

    private BlockingQueue<Update> imageQueue;
    private Thread cmdPump;
    private ParcelFileDescriptor usbFd;
    private ViredroidRenderer renderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "onCreate");
        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        renderer = new ViredroidRenderer(this);
    }

    private ParcelFileDescriptor getUsbFd(UsbManager manager) throws SecurityException {
        UsbAccessory[] accessories = manager.getAccessoryList();
        // This array is either null or contains exactly one element
        //  great API design!
        if (accessories == null) {
            return null;
        }
        UsbAccessory accessory = accessories[0];
        //TODO: should be changed to url so other implementations are covered
        if ("x-viredero".equals(accessory.getModel())) {
            return manager.openAccessory(accessory);
        }
        return null;
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        renderer.onSurfaceCreated(config);

        terminateCmdPump();

        imageQueue = new ArrayBlockingQueue<Update>(10);

        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        try {
            usbFd = getUsbFd(manager);
        } catch (SecurityException se) {
            Log.w(LOGTAG, "Restarting due to security exception");
            return;
        }
        if (usbFd == null) {
            Log.w(LOGTAG, "USB not yet connected! Please connect usb and come back");
            return;
        }
        Log.d(LOGTAG, "got usb fd!");
        Runnable r = new UsbCmdPump(imageQueue, renderer, renderer.getScreenTexDataHandle()
                                    , renderer.getPointerTexDataHandle(), usbFd);
        cmdPump = new Thread(r);
        cmdPump.start();
    }

    public String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = reader.readLine();
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("Read file failed");
        }
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        lastHeadXform = headTransform;
    }

    @Override
    public void onDrawEye(Eye eye) {
        Log.d(LOGTAG, "onDrawEye");
        List<Update> updates = new ArrayList<Update>(imageQueue.size());
        imageQueue.drainTo(updates);
        renderer.onDrawEye(eye, updates);
        if (!cmdPump.isAlive()) {
            onStop();
        }
    }

    @Override
    public void onCardboardTrigger() {
        renderer.recenter(lastHeadXform);
        vibrator.vibrate(50);
    }

    private void terminateCmdPump() {
        if (cmdPump == null) {
            return;
        }
        cmdPump.interrupt();
        while (cmdPump.isAlive()) {
            try {
                cmdPump.join();
            } catch (InterruptedException ie) {
            }
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        Log.i(LOGTAG, "onStop");
        terminateCmdPump();
        if (usbFd != null) {
            try {
                usbFd.close();
            } catch(IOException e) {
            }
        }
        finish();
    }
    @Override
    public void onRendererShutdown() {
    }
    @Override
    public void onSurfaceChanged(int width, int height) {
    }
    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    public static void handleError(String errorText) {
        //TODO: something nice should be here, like friendly text, graceful exit
        //       and angry email to developers
        throw new RuntimeException(errorText);
    }

}

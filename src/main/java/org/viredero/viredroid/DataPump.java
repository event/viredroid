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
import android.opengl.Matrix;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.FloatMath;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.lang.Runnable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class DataPump implements Runnable {

    private static final String TAG = "viredroid";

    private static final String IPADDR = "192.168.100.132";
    private static final int PORT = 5003;

    private Map<Integer, Command>  commandMap;
    public DataPump(){
        commandMap = new HashMap<Integer, Command>();
        commandMap.put(1, new InitCmd());
        commandMap.put(2, new ImageCmd());
        commandMap.put(3, new DistanceCmd());
        commandMap.put(4, new MouseCmd());
    }

    public void run() {
        InetAddress addr = InetAddress.getByName(IPADDR);
        Socket s = new Socket(addr, PORT);
        InputStream stream = s.getInputStream();
        while (true) {
            int code = stream.read();
            Command cmd = commandMap.get(code);
            if (run != null) {
                cmd.exec(stream);
            } else {
                Log.e(TAG, "Received unknown command " + code);
            }
        }
                         
    }
}

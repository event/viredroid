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

import android.util.Log;
import android.os.ParcelFileDescriptor;

import java.lang.Runnable;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class UsbDataPump implements Runnable {

    private static final String TAG = "viredroid";
    private InputStream stream;
    private Map<Integer, Command> commandMap;
    public UsbDataPump(int targetId, BlockingQueue<ImageUpdate> queue, ParcelFileDescriptor fd){
        commandMap = new HashMap<Integer, Command>();
        commandMap.put(1, new InitCmd());
        commandMap.put(2, new ImageCmd(targetId, queue));
        commandMap.put(3, new DistanceCmd());
        commandMap.put(4, new MouseCmd());
        stream = new FileInputStream(fd.getFileDescriptor());
    }

    public void run() {
        try {
            while (true) {
                int code = 0;
                code = stream.read();
                Command cmd = commandMap.get(code);
                if (cmd != null) {
                    cmd.exec(stream);
                } else {
                    throw new RuntimeException("Received unknown command " + code);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "io failure", e);
        }

    }
}

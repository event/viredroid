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

import java.lang.Runnable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import android.util.Log;

public class NetCmdPump extends AbstractCmdPump {

    private static final String TAG = "viredroid";

    private final InetAddress addr;
    private final int port;

    public NetCmdPump(BlockingQueue<Update> queue, int screenTexDataHandle
                      , int pointTexDataHandle, String addr, int port){
        super(queue, screenTexDataHandle, pointTexDataHandle);
        this.port = port;
        try {
            this.addr = InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
            throw new RuntimeException("failed to resolve '" + addr + "'", e);
        }
    }

    @Override
    public InputStream createIS() throws IOException {
        Socket s = new Socket(addr, port);
        return s.getInputStream();
    }
}

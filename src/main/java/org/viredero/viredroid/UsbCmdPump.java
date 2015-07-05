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
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class UsbCmdPump extends AbstractCmdPump {

    private static final String TAG = "viredroid";
    private ParcelFileDescriptor fd;
    public UsbCmdPump(BlockingQueue<Update> queue, int screenTexDataHandle
                      , int pointTexDataHandle, ParcelFileDescriptor fd){
        super(queue, screenTexDataHandle, pointTexDataHandle);
        this.fd = fd;
    }

    @Override
    public InputStream createIS() throws IOException {
        return new FixedReadBufferedInputStream(
            new FileInputStream(fd.getFileDescriptor()), 16384);
    }

    @Override
    public OutputStream createOS() throws IOException {
        return new FileOutputStream(fd.getFileDescriptor());
    }
    
}

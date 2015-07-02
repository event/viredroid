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
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractCmdPump implements Runnable {

    private static final String TAG = "viredroid";
    private List<Command> commands;
    private BlockingQueue<Update> queue;
    private InputStream is;
    private int screenTexDataHandle;
    private int pointTexDataHandle;
    
    public AbstractCmdPump(BlockingQueue<Update> queue, int screenTexDataHandle
                           , int pointTexDataHandle){
        this.queue = queue;
        this.screenTexDataHandle = screenTexDataHandle;
        this.pointTexDataHandle = pointTexDataHandle;
    }

    private void initCommands() {
        try {
            this.is = createIS();
        } catch (IOException ioe) {
            throw new RuntimeException("Building stream failed", ioe);
        }
        commands = new ArrayList<Command>(4);
        commands.add(new ErrorCmd()); // we send Init, not receive it
        commands.add(new InitReplyCmd(is));
        commands.add(new ImageCmd(is, screenTexDataHandle));
        commands.add(new PointerCmd(is, pointTexDataHandle, 6, 6));
        commands.add(new DistanceCmd(is));
    }

    @Override
    public void run(){
        initCommands();
        try {
            while (true) {
                int code = is.read();
                if (code < 0 || code >= commands.size()) {
                    throw new RuntimeException("Received unknown command " + code);
                }
                Command cmd = commands.get(code);
                Update u = cmd.exec();
                if (u != null) {
                    queue.offer(u);
                }
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Error while pumping commands", ioe);
        }
    }

    public abstract InputStream createIS() throws IOException;
}

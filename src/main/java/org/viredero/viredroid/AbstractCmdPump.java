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
import java.lang.InterruptedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractCmdPump implements Runnable {

    private static final int VIREDERO_PROTO_VERSION = 1;
    private static final int SCREEN_FMT_RGB = 1;
    private static final int POINTER_FMT_RGBA = 1; 
    private static final int INIT_REPLY_CMD_IDX = 1;

    private List<Command> commands;
    private BlockingQueue<Update> queue;
    private InputStream is;
    private int screenTexDataHandle;
    private int pointTexDataHandle;
    private ViredroidRenderer renderer;
    private int screenWidth;
    private int screenHeight;
    
    public AbstractCmdPump(BlockingQueue<Update> queue, ViredroidRenderer renderer
                           , int screenTexDataHandle, int pointTexDataHandle){
        this.queue = queue;
        this.renderer = renderer;
        this.screenTexDataHandle = screenTexDataHandle;
        this.pointTexDataHandle = pointTexDataHandle;
    }

    public abstract InputStream createIS() throws IOException;
    public abstract OutputStream createOS() throws IOException;

    private void initCommands() throws IOException {
        commands = new ArrayList<Command>(4);
        commands.add(new ErrorCmd()); // we send Init, not receive it
        commands.add(new InitReplyCmd(this, is, screenTexDataHandle, pointTexDataHandle));
        commands.add(new ImageCmd(this, is, screenTexDataHandle));
        commands.add(new PointerCmd(this, is, pointTexDataHandle));
        commands.add(new DistanceCmd(is));
    }

    @Override
    public void run() {
        Log.d(ViredroidGLActivity.LOGTAG, "Starting cmd pump");
        try {
            do_run();
        } catch (Throwable t) {
            Log.i(ViredroidGLActivity.LOGTAG, "Pump terminated due to exception", t);
        }
    }

    private void runCmd(Command cmd, boolean exec) throws IOException {
        if (! exec) {
            cmd.skip();
            return;
        }
        Update u = cmd.exec();
        if (u != null) {
            queue.offer(u);
        }
    }

    private void do_run() throws IOException{
        boolean initFinished = false;
        ExecutorService execServ = Executors.newSingleThreadExecutor();
        is = createIS();
        is.skip(is.available()); //cleanup
        initPeer();
        initCommands();
        while (! Thread.interrupted()) {
            Callable<Integer> read = () -> {return is.read();};
            Future<Integer> fRead = execServ.submit(read);
            int code = 0;
            boolean readSucceed = false;
            try {
                while (! readSucceed) {
                    try {
                        code = fRead.get(10, TimeUnit.SECONDS);
                        readSucceed = true;
                    } catch (TimeoutException te) {
                        Log.w(ViredroidGLActivity.LOGTAG, "Long time since last command");
                        queue.offer(new IndicateNoCmdUpdate(screenTexDataHandle, screenWidth, screenHeight));
                        code = 0; // avoid compile error
                    }
                }
            } catch (Exception e) {
                Log.w(ViredroidGLActivity.LOGTAG, "Failed", e);
                return;
            }
            Log.d(ViredroidGLActivity.LOGTAG, String.format("Received command code %d", code));
            if (code < 0 || code >= commands.size()) {
                throw new RuntimeException("Received unknown command " + code);
            }
            initFinished |= code == INIT_REPLY_CMD_IDX; // init is finished as soon as we got initReplyCmd
            runCmd(commands.get(code), initFinished);
        }
    }

    private void initPeer() throws IOException {
        OutputStream os = createOS();
        if (os == null) {
            Log.i(ViredroidGLActivity.LOGTAG
                  , "Aborting peer initialization: output stream not created");
            return;
        }
        os.write(0); //init cmd code
        os.write(VIREDERO_PROTO_VERSION);
        os.write(SCREEN_FMT_RGB);    // OR'ed screen image format constants
        os.write(POINTER_FMT_RGBA);  // OR'ed pointer image format constants
        os.flush();
        os.close();
    }

    public void setDimentions(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        renderer.setDimentions(width, height);
    }

    public int getWidth() {
        return screenWidth;
    }

    public int getHeight() {
        return screenHeight;
    }
}

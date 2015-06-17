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

import android.opengl.GLES20;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.BlockingQueue;

public class ImageCmd implements Command {
    private static final String TAG = "viredroid";
    private static final int BYT_PER_PIX = 4;

    private int targetId;
    private BlockingQueue<ImageUpdate> queue;
    
    public ImageCmd(int targetId, BlockingQueue<ImageUpdate> queue) {
        this.targetId = targetId;
        this.queue = queue;
    }

    public void exec(InputStream s) throws IOException {
        DataInputStream dis = new DataInputStream(s);
        int width = dis.readInt();
        int height = dis.readInt();
        int xOffset = dis.readInt();
        int yOffset = dis.readInt();
        int imageSize = 4 * width * height;
        if (imageSize <= 0) {
            throw new RuntimeException("image size < 0");
        }
        ByteBuffer imageBuf = ByteBuffer.allocateDirect(imageSize)
            .order(ByteOrder.nativeOrder());
        byte[] buf = imageBuf.array();
        int totalRead = 0;
        while (imageSize > 0) {
            int read = s.read(buf, totalRead, imageSize);
            if (read < 0) {
                throw new RuntimeException("Sudden end of stream!");
            }
            imageSize -= read;
            totalRead += read;
        }
        imageBuf.position(0);
        queue.offer(new ImageUpdate(width, height, xOffset, yOffset, imageBuf));
    }
}

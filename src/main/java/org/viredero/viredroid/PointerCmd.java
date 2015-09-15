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

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import android.util.Log;

public class PointerCmd implements Command {

    private int pointerTexDataHandle;
    private DataInputStream dis;
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    private int eraseWidth = 0;
    private int eraseHeight = 0;
    private ByteBuffer eraseImageBytes;
    private ByteBuffer pointerImageBytes;
    private AbstractCmdPump cmdPump;
    
    public PointerCmd(AbstractCmdPump cmdPump, InputStream s
                      , int pointerTexDataHandle) {
        this.pointerTexDataHandle = pointerTexDataHandle;
        this.dis = new DataInputStream(s);
        this.cmdPump = cmdPump;
    }

    @Override
    public Update exec() throws IOException {
        PointerUpdate erase = new PointerUpdate(
            pointerTexDataHandle, eraseWidth, eraseHeight
            , x, y, eraseImageBytes);
        x = dis.readInt();
        y = dis.readInt();
        boolean hasCursor = dis.readBoolean();
        if (hasCursor) {
            int rWidth = dis.readInt();
            int rHeight = dis.readInt();
            int imageSize = 4 * rWidth * rHeight;
            if (imageSize <= 0) {
                throw new RuntimeException("image size <= 0");
            }
            if (width != rWidth || height != rHeight) {
                eraseImageBytes = ByteBuffer.allocateDirect(imageSize)
                    .order(ByteOrder.nativeOrder());
                Arrays.fill(eraseImageBytes.array(), (byte)0);
                pointerImageBytes = ByteBuffer.allocateDirect(imageSize)
                    .order(ByteOrder.nativeOrder());
            }
            byte[] buf = pointerImageBytes.array();
            int totalRead = 0;
            while (imageSize > 0) {
                int read = dis.read(buf, totalRead, imageSize);
                if (read < 0) {
                    throw new RuntimeException("Sudden end of stream!");
                }
                imageSize -= read;
                totalRead += read;
            }
            pointerImageBytes.position(0);
            width = rWidth;
            height = rHeight;
        } else if (width == 0) {
            throw new RuntimeException("Cannot draw empty pointer");
        }
        eraseWidth = limitDim(x, width, cmdPump.getWidth());
        eraseHeight = limitDim(y, height, cmdPump.getHeight());
        ByteBuffer pntr = pointerImageBytes;
        if (eraseWidth != width || eraseHeight != height) {
            pntr = ByteBuffer
                .allocateDirect(4 * eraseWidth * eraseHeight)
                .order(ByteOrder.nativeOrder());
            byte[] buf = pntr.array();
            for (int i = 0; i < eraseHeight; i += 1) {
                pointerImageBytes.position(width * i * 4);
                pointerImageBytes.get(buf, eraseWidth * i * 4, eraseWidth * 4);
            }
            pointerImageBytes.position(0);
        }
        return new MultiUpdate(erase, new PointerUpdate(
                                   pointerTexDataHandle, eraseWidth, eraseHeight
                                   , x, y, pntr));
    }

    private int limitDim(int offset, int val, int limit) {
        if (offset + val <= limit) {
            return val;
        } else {
            return limit - offset;
        }
    }
}

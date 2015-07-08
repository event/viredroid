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
    private static final String TAG = "viredroid";

    private int pointerTexDataHandle;
    private DataInputStream dis;
    private int x = 0;
    private int y = 0;
    private int width = 0;
    private int height = 0;
    private ByteBuffer eraseImageBytes;
    private ByteBuffer pointerImageBytes;
    
    public PointerCmd(InputStream s, int pointerTexDataHandle) {
        this.pointerTexDataHandle = pointerTexDataHandle;
        this.dis = new DataInputStream(s);
    }

    @Override
    public Update exec() throws IOException {
        Update u = null;
        int newX = dis.readInt();
        int newY = dis.readInt();
        boolean hasCursor = dis.readBoolean();
        if (hasCursor) {
            int newWidth = dis.readInt();
            int newHeight = dis.readInt();
            int imageSize = 4 * newWidth * newHeight;
            if (imageSize <= 0) {
                throw new RuntimeException("image size <= 0");
            }
            pointerImageBytes = ByteBuffer.allocateDirect(imageSize)
                .order(ByteOrder.nativeOrder());
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
            if (eraseImageBytes != null) {
                eraseImageBytes.position(0);
            }
            
            u = new MultiUpdate(
                new PointerUpdate(pointerTexDataHandle, width, height
                                  , x, y, eraseImageBytes)
                , new PointerUpdate(pointerTexDataHandle, newWidth, newHeight
                                    , newX, newY, pointerImageBytes));
            if (width != newWidth || height != newHeight) {
                eraseImageBytes = ByteBuffer.allocateDirect(imageSize)
                    .order(ByteOrder.nativeOrder());
                Arrays.fill(eraseImageBytes.array(), (byte)0);
            }
            width = newWidth;
            height = newHeight;
        } else if (width == 0) {
            throw new RuntimeException("Cannot draw empty pointer");
        } else if (x != newX || y != newY){
            u = new MultiUpdate(
                new PointerUpdate(pointerTexDataHandle, width, height
                                  , x, y, eraseImageBytes)
                , new PointerUpdate(pointerTexDataHandle, width, height
                                    , newX, newY, pointerImageBytes));
        }
        x = newX;
        y = newY;
        return u;
    }
}

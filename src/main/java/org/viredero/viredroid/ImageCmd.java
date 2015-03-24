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

import java.net.Socket;

public class ImageCmd implements Command {
    public void exec(InputStream s) {
        byte[512] buf;
        s.read(buf, 0, 16);
        int width = buf[0] << 24 | buf[1] << 16 | buf[2] << 8 | buf[3];
        int height = buf[4] << 24 | buf[5] << 16 | buf[6] << 8 | buf[7];
        int xOffset = buf[8] << 24 | buf[9] << 16 | buf[10] << 8 | buf[11];
        int yOffset = buf[12] << 24 | buf[13] << 16 | buf[14] << 8 | buf[15];
        int imageSize = 3 * width * height;
        ByteBuffer imageBuf = ByteBuffer.allocateDirect(imageSize)
            .order(ByteOrder.nativeOrder());
        while (imageSize > 0) {
            int cnt = Math.min(512, imageSize);
            int read = s.read(buf, 0, cnt);
            if (read < 0) {
                throw new RuntimeException("Sudden end of stream!");
            }
            imageSize -= read;
            imageBuf.put(buf, 0, read);
        }
        GLES20.glTexSubImage2D(targetId, 0, xOffset, yOffset
                               , width, height, GLES20.GL_RGB
                               , GLES20.GL_UNSIGNED_BYTE, imageBuf);
    }
}

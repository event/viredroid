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

import java.nio.ByteBuffer;
import android.opengl.GLES20;

public class SubImageUpdate implements Update {
    private final int texId;
    private final int format;
    private final int width;
    private final int height;
    private final int xOffset;
    private final int yOffset;
    private final ByteBuffer bytes;

    public SubImageUpdate(int texId, int format, int width, int height
                          , int xOffset, int yOffset, ByteBuffer bytes) {
        this.texId = texId;
        this.format = format;
        this.width = width;
        this.height = height;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.bytes = bytes;
    }

    @Override
    public void draw() {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
        GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, xOffset
                               , yOffset, width, height, format
                               , GLES20.GL_UNSIGNED_BYTE, bytes);
    }
}

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
import java.util.Arrays;

public class SetupScreen implements Update {
    private final int width;
    private final int height;
    private final int screenTexId;
    private final int pointerTexId;

    public SetupScreen(int screenTexId, int pointerTexId, int width
                       , int height) {
        this.width = width;
        this.height = height;
        this.screenTexId = screenTexId;
        this.pointerTexId = pointerTexId;
    }

    private void fillAndDraw(int texId, int format, int bytes_per_pix, byte value) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId);
 
        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D
                               , GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D
                               , GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
 
        ByteBuffer imageBuf = ByteBuffer.allocateDirect(bytes_per_pix * width * height);
        byte[] buf = imageBuf.array();
        Arrays.fill(buf, value);
        imageBuf.position(0);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format
                            , width, height, 0, format
                            , GLES20.GL_UNSIGNED_BYTE, imageBuf);
    }
    
    @Override
    public void draw() {
        fillAndDraw(screenTexId, GLES20.GL_RGB, 3, (byte)32);
        fillAndDraw(pointerTexId, GLES20.GL_RGBA, 4, (byte)0);
    }
}

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
import java.nio.ByteBuffer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.opengl.GLES20;

public class PNGScreenUpdate extends SubImageUpdate {

    public PNGScreenUpdate(int texId, int width, int height, int xOffset
                           , int yOffset, ByteBuffer bytes) {
        super(texId, GLES20.GL_RGB, width, height, xOffset, yOffset
              , decodePng(bytes));
    }

    private static ByteBuffer decodePng(ByteBuffer png) {
        Bitmap b = BitmapFactory.decodeByteArray(png.array(), 0, png.limit());
        int size = b.getRowBytes() * b.getHeight();
        ByteBuffer out = ByteBuffer.allocateDirect(size);
        b.copyPixelsToBuffer(out);
        byte[] a = out.array();
        int rdIdx = 0;
        int wrIdx = 0;
        while (rdIdx < size) {
            a[wrIdx] = a[rdIdx + 2];
            a[wrIdx + 1] = a[rdIdx];
            a[wrIdx + 2] = a[rdIdx + 1];
            wrIdx += 3;
            rdIdx += 4;
        }
        b.recycle();
        out.position(0);
        return out;
    }

}

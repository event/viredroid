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

public class IndicateNoCmdUpdate extends MultiUpdate {
    private static final int BORDER_WIDTH = 10;
    private static final int MAX_PIXELS = 10000 * BORDER_WIDTH;
    private static final ByteBuffer BORDERS_BYTES;
    
    static {
        BORDERS_BYTES = ByteBuffer.allocateDirect(MAX_PIXELS * 3);
        byte[] buf = BORDERS_BYTES.array();
        for (int i = 0; i < MAX_PIXELS; i += 1) {
            buf[3*i + 1] = 127;
            buf[3*i] = buf[3*i + 2] = 0;
        }
    }
    
    public IndicateNoCmdUpdate(int texId, int screenWidth, int screenHeight) {
        super(new ScreenUpdate(texId, BORDER_WIDTH, screenHeight, 0, 0, BORDERS_BYTES)
              , new ScreenUpdate(texId, screenWidth, BORDER_WIDTH, 0, screenHeight - BORDER_WIDTH, BORDERS_BYTES)
              , new ScreenUpdate(texId, BORDER_WIDTH, screenHeight, screenWidth - BORDER_WIDTH, 0, BORDERS_BYTES)
              , new ScreenUpdate(texId, screenWidth, BORDER_WIDTH, 0, 0, BORDERS_BYTES));
    }

}

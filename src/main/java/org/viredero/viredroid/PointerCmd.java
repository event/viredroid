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

public class PointerCmd implements Command {
    private static final String TAG = "viredroid";

    private int pointerTexDataHandle;
    private DataInputStream dis;
    private static int oldX = 0;
    private static int oldY = 0;
    private final int width;
    private final int height;
    
    public PointerCmd(InputStream s, int pointerTexDataHandle, int width, int height) {
        this.pointerTexDataHandle = pointerTexDataHandle;
        this.dis = new DataInputStream(s);
        this.width = width;
        this.height = height;
    }

    @Override
    public Update exec() throws IOException {
        int x = dis.readInt();
        int y = dis.readInt();
        Update u = new PointerUpdate(pointerTexDataHandle, x, y, oldX, oldY, width, height);
        oldX = x;
        oldY = y;
        return u;
    }
}

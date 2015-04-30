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

public class ImageUpdate {
    private final int width;
    private final int height;
    private final int xOffset;
    private final int yOffset;
    private final ByteBuffer bytes;

    public ImageUpdate(int width, int height, int xOffset, int yOffset, ByteBuffer bytes) {
        this.width = width;
        this.height = height;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.bytes = bytes;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getXOffset() {
        return xOffset;
    }
                            
    public int getYOffset() {
        return yOffset;
    }
                            
    public ByteBuffer getBytes() {
        return bytes;
    }
                            
}

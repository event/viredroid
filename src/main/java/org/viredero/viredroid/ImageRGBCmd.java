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
import java.nio.ByteBuffer;

public class ImageRGBCmd extends BaseImageCmd {

    public ImageRGBCmd(AbstractCmdPump cmdPump, InputStream s
                       , int screenTexDataHandle) {
        super(cmdPump, s, screenTexDataHandle);
    }

    public Update getScreenUpdate(int width, int height
                                  , int xOffset, int yOffset
                                  , ByteBuffer imageBuf) {
        return new RGBScreenUpdate(getScreenTexDataHandle(), width, height
                                , xOffset, yOffset, imageBuf);
    }
}

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
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with viredero; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package org.viredero.viredroid;

import java.io.InputStream;
import java.io.IOException;

public class InitReplyCmd implements Command {

    private InputStream is;

    public InitReplyCmd(InputStream is) {
        this.is = is;
    }

    @Override
    public Update exec() throws IOException {
        int res = is.read();
        InitReplyResultCode code = InitReplyResultCode.fromInt(res);
        if (code != InitReplyResultCode.SUCCESS) {
            throw new RuntimeException("Handshake failed: " + code.getMessage());
        }
        int scrFmt = is.read();
        int pntrFmt = is.read();
        return null;
    }
}

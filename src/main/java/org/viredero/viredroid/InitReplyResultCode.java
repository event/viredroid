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

import java.util.Map;
import java.util.HashMap;

public enum InitReplyResultCode {
    SUCCESS(0, ""),
    ERROR_BAD_MESSAGE(1, "Bad Init message"),
    ERROR_VERSION(2, "Version mismatch"),
    ERROR_SCREEN_FORMAT_NOT_SUPPORTED(3, "Screen format mismatch"),
    ERROR_POINTER_FORMAT_NOT_SUPPORTED(4, "Pointer format mismatch"),
    ERROR_UNKNOWN(999, "Unknown error");

    private static final Map<Integer, InitReplyResultCode>
    Code2Int = new HashMap<Integer, InitReplyResultCode>();

    static {
        for (InitReplyResultCode c : InitReplyResultCode.values()) {
            Code2Int.put(c.code, c);
        }
    }
    private final int code;
    private final String message;

    InitReplyResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
    
    public static InitReplyResultCode fromInt(int i) {
        InitReplyResultCode res = Code2Int.get(i);
        if (res == null) {
            return InitReplyResultCode.ERROR_UNKNOWN;
        }
        return res;
    }
}

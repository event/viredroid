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

import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.HeadTransform;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbAccessory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.lang.StringBuilder;

import javax.microedition.khronos.egl.EGLConfig;

public class ViredroidRenderer {

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 3f;

    private static final int COORDS_PER_VERTEX = 3;
    private static final int COORDS_PER_TEXTURE = 2;
    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;

    private static final float BASE_DISTANCE = 1f;
    private static final float BASE_WIDTH = 1024f;
    private static final float BASE_SCREEN_LEVITATION = 2f;

    private static final int SCREEN_SLICES = 50; // horizontal segments
    private static final int SCREEN_STACKS = 50; // vertical segments
    // screen is semicircle in front of the user. Depth goes from zero on sides to MAX_DEPTH in the center
    private static final float SCREEN_WIDTH = 16f;
    private static final float SCREEN_HEIGHT = 9f;
    private static final float SCREEN_MAX_DEPTH = -8f;

    private static final float FLOOR_DEPTH = 20f;
    public static final float[] FLOOR_COORDS = new float[] {
        200f, 0, -200f,
        -200f, 0, -200f,
        -200f, 0, 200f,
        200f, 0, -200f,
        -200f, 0, 200f,
        200f, 0, 200f,
    };

    public static final float[] FLOOR_NORMALS = new float[] {
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
        0.0f, 1.0f, 0.0f,
    };

    public static final float[] FLOOR_COLOR = new float[] {
        0.0f, 0.3398f, 0.9023f, 1.0f};

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {
        0.0f, 2.0f, 0.0f, 1.0f };

    private float screenDistance = BASE_DISTANCE;
    private final float[] lightPosInEyeSpace = new float[4];

    private final float[] modelScreen = new float[16];
    private final float[] modelViewProjection = new float[16];
    private final float[] modelView = new float[16];
    private final float[] modelFloor = new float[16];

    private FloatBuffer floorVertices;
    private FloatBuffer floorNormals;

    private FloatBuffer screenVertices;
    private ShortBuffer screenIndices;
    private FloatBuffer screenTextures;
    private int screenIndicesSize;

    private int screenProgram;
    private int floorProgram;

    private int screenPositionParam;
    private int screenTexParam;
    private int screenModelViewProjectionParam;
    private int screenTexUnihandle;
    private int pointTexUnihandle;
 
    private int screenTexDataHandle;
    private int pointerTexDataHandle;

    private int floorPositionParam;
    private int floorNormalParam;
    private int floorModelParam;
    private int floorModelViewParam;
    private int floorModelViewProjectionParam;
    private int floorLightPosParam;
    private int floorColorUParam;

    private ViredroidGLActivity activity;

    public ViredroidRenderer(ViredroidGLActivity activity) {
        this.activity = activity;
    }

    private void checkGLError() {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String errorText = new StringBuilder("glError ").append(error)
                .append(": ").append(GLU.gluErrorString(error)).toString();
            activity.handleError(errorText);
        }
    }

    public void onSurfaceCreated(EGLConfig config) {
        Log.i(ViredroidGLActivity.LOGTAG, "Creating scene");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f);
        fillScreenCoords();
        // make a floor
        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(FLOOR_COORDS.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        floorVertices = bbFloorVertices.asFloatBuffer();
        floorVertices.put(FLOOR_COORDS);
        floorVertices.position(0);

        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(FLOOR_NORMALS.length * 4);
        bbFloorNormals.order(ByteOrder.nativeOrder());
        floorNormals = bbFloorNormals.asFloatBuffer();
        floorNormals.put(FLOOR_NORMALS);
        floorNormals.position(0);

        int floorVertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int screenVertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.screen_vertex);
        int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER
                                             , R.raw.passthrough_fragment);
        screenProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(screenProgram, screenVertexShader);
        GLES20.glAttachShader(screenProgram, passthroughShader);
        GLES20.glLinkProgram(screenProgram);
        GLES20.glUseProgram(screenProgram);

        screenTexDataHandle = newTexture();
        pointerTexDataHandle = newTexture();
        checkGLError();

        screenPositionParam = GLES20.glGetAttribLocation(screenProgram, "a_Position");
        screenTexParam = GLES20.glGetAttribLocation(screenProgram, "a_TexCoord");

        screenModelViewProjectionParam = GLES20.glGetUniformLocation(screenProgram, "u_MVP");
        screenTexUnihandle = GLES20.glGetUniformLocation(screenProgram, "u_TexScreen");
        pointTexUnihandle = GLES20.glGetUniformLocation(screenProgram, "u_TexPointer");

        GLES20.glEnableVertexAttribArray(screenPositionParam);
        GLES20.glEnableVertexAttribArray(screenTexParam);

        checkGLError();

        floorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(floorProgram, floorVertexShader);
        GLES20.glAttachShader(floorProgram, gridShader);
        GLES20.glLinkProgram(floorProgram);
        GLES20.glUseProgram(floorProgram);

        checkGLError();

        floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
        floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");
        floorColorUParam = GLES20.glGetUniformLocation(floorProgram, "u_Color");

        floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
        floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");

        GLES20.glEnableVertexAttribArray(floorPositionParam);
        GLES20.glEnableVertexAttribArray(floorNormalParam);

        checkGLError();

        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Object first appears directly in front of user.
        Matrix.setIdentityM(modelScreen, 0);
        Matrix.translateM(modelScreen, 0, 0, BASE_SCREEN_LEVITATION, -screenDistance);

        Matrix.setIdentityM(modelFloor, 0);
        Matrix.translateM(modelFloor, 0, 0, -FLOOR_DEPTH, 0); // Floor appears below user.

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        checkGLError();
        Log.i(ViredroidGLActivity.LOGTAG, "Scene created");
    }
    

    private void fillScreenCoords() {
        screenVertices = ByteBuffer.allocateDirect(
            SCREEN_STACKS * SCREEN_SLICES * COORDS_PER_VERTEX * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        screenTextures = ByteBuffer.allocateDirect(
            SCREEN_STACKS * SCREEN_SLICES * COORDS_PER_TEXTURE * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        screenIndices = ByteBuffer.allocateDirect(
            (SCREEN_STACKS - 1) * 2 * (SCREEN_SLICES + 2) * BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder()).asShortBuffer();

        float[] ys = new float[SCREEN_STACKS];
        float[] xs = new float[SCREEN_SLICES];
        float[] zs = new float[SCREEN_SLICES];
        float[] dws = new float[SCREEN_SLICES];
        float dx = SCREEN_WIDTH / (SCREEN_SLICES - 1) * 2f;
        float dy = SCREEN_HEIGHT / (SCREEN_STACKS - 1) * 2f;
        ys[0] = SCREEN_HEIGHT;
        for (int i = 1; i < SCREEN_STACKS; i += 1) {
            ys[i] = ys[i-1] - dy;
        }
        float realW = 0f;
        float horSector = (float)Math.PI / (SCREEN_SLICES - 1);
        xs[0] = SCREEN_WIDTH;
        zs[0] = 0f;
        dws[0] = dx;
        for (int i = 1; i < SCREEN_SLICES; i += 1) {
            xs[i] = xs[i-1] - dx;
            zs[i] = (float)Math.sin(i * horSector) * SCREEN_MAX_DEPTH;
            float dz = zs[i - 1] - zs[i];
            float dw = (float)Math.sqrt(dx * dx + dz * dz);
            dws[i] = dw;
            realW += dw;
        }
        float revDblHeight = .5f / SCREEN_HEIGHT;
        float texU = .995f;
        for (int i = 0; i < SCREEN_STACKS; i += 1) {
            float texV = 1 - (ys[i] + SCREEN_HEIGHT) * revDblHeight;
            for (int j = 0; j < SCREEN_SLICES; j += 1) {
                screenVertices.put(xs[j]);
                screenVertices.put(ys[i]);
                screenVertices.put(zs[j]);
                if (i > 0) { //take one from previous row
                    screenTextures.put(screenTextures.get((((i-1) * SCREEN_SLICES) + j) * 2));
                } else {
                    screenTextures.put(texU);
                    texU -= dws[j]/realW;
                }
                screenTextures.put(texV);
            }
            if (i < SCREEN_STACKS - 1) {
                if (i > 0) {
                    // Degenerate begin: repeat first vertex
                    screenIndices.put((short)(i * SCREEN_SLICES));
                }
                for (int j = 0; j < SCREEN_SLICES; j += 1) {
                    // One part of the strip
                    screenIndices.put((short)(i * SCREEN_SLICES + j));
                    screenIndices.put((short)((i + 1) * SCREEN_SLICES + j));
                }
                if (i < SCREEN_STACKS - 2) {
                    // Degenerate end: repeat last vertex
                    screenIndices.put(screenIndices.get(screenIndices.position() - 1));
                }
            }
        }
        screenIndicesSize = screenIndices.position();
        screenIndices.position(0);
        screenVertices.position(0);
        screenTextures.position(0);
    }
    
    private int newTexture() {
        int[] textureHandle = new int[1];
 
        GLES20.glGenTextures(1, textureHandle, 0);
 
        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error creating texture");
        }
 
        return textureHandle[0];
    }

    public void onDrawEye(Eye eye, List<Update> updates) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError();

        float[] eyeView = eye.getEyeView();
        Matrix.multiplyMV(lightPosInEyeSpace, 0, eyeView, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(modelView, 0, eyeView, 0, modelFloor, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0,
                          modelView, 0);
        drawFloor();

        Matrix.multiplyMM(modelView, 0, eyeView, 0, modelScreen, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawScreen(updates);

    }

    private void drawScreen(List<Update> updates) {
        GLES20.glUseProgram(screenProgram);

        for (Update update : updates) {
            update.draw();
            Log.d(ViredroidGLActivity.LOGTAG, "updated");
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, screenTexDataHandle);
        GLES20.glUniform1i(screenTexUnihandle, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pointerTexDataHandle);
        GLES20.glUniform1i(pointTexUnihandle, 1);

        GLES20.glVertexAttribPointer(
            screenPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT
            , false, 0, screenVertices);

        GLES20.glVertexAttribPointer(
            screenTexParam, COORDS_PER_TEXTURE
            , GLES20.GL_FLOAT, false, 0, screenTextures);

        GLES20.glUniformMatrix4fv(
            screenModelViewProjectionParam, 1
            , false, modelViewProjection, 0);

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLE_STRIP, screenIndicesSize
            , GLES20.GL_UNSIGNED_SHORT, screenIndices);
 
        checkGLError();
    }

    private void drawFloor() {
        GLES20.glUseProgram(floorProgram);

        GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniform4fv(floorColorUParam, 1, FLOOR_COLOR, 0);
        GLES20.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
        GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false,
                                  modelViewProjection, 0);
        GLES20.glVertexAttribPointer(floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                                     false, 0, floorVertices);
        GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0,
                                     floorNormals);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        checkGLError();
    }
    
    private int loadGLShader(int type, int resId) {
        String code = activity.readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        if (compileStatus[0] == 0) {
            throw new RuntimeException("Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
        }
        return shader;
    }

    public void recenter(HeadTransform headPos) {
        float[] headView = new float[16];
        headPos.getHeadView(headView, 0);
        Matrix.invertM(modelScreen, 0, headView, 0);
        System.arraycopy(modelScreen, 0, modelFloor, 0, 16);
        Matrix.translateM(modelScreen, 0, 0, BASE_SCREEN_LEVITATION, -screenDistance);
        Matrix.translateM(modelFloor, 0, 0, -FLOOR_DEPTH, 0); // Floor appears below user.
    }

    public int getScreenTexDataHandle() {
        return screenTexDataHandle;
    }

    public int getPointerTexDataHandle() {
        return pointerTexDataHandle;
    }

    public void setDimentions(int width, int height) {
        float scale = BASE_WIDTH < width ? BASE_WIDTH / width : 1.0f;
        screenDistance = BASE_DISTANCE + BASE_DISTANCE * scale;
        Matrix.setIdentityM(modelScreen, 0);
        Matrix.translateM(modelScreen, 0, 0, BASE_SCREEN_LEVITATION, -screenDistance);
    }
}

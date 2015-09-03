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

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.CardboardDeviceParams;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * A Cardboard sample application.
 */
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    private static final String TAG = "viredroid";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 3f;

    private static final int COORDS_PER_VERTEX = 3;
    private static final int COORDS_PER_TEXTURE = 2;
    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;

    private static final float FLOOR_DEPTH = 20f;
    // We keep the light always position just above the user.
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

    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] {
        0.0f, 2.0f, 0.0f, 1.0f };

    private final float[] lightPosInEyeSpace = new float[4];

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

    private float[] modelScreen;
    private float[] camera;
    private float[] sceneView;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelFloor;

    HeadTransform lastHeadXform;
    
    private float objectDistance = 10f;

    private Vibrator vibrator;

    private BlockingQueue<Update> imageQueue;
    private Thread cmdPump;
    private ParcelFileDescriptor usbFd;
    
    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    private static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error + " " + GLU.gluErrorString(error));
            throw new RuntimeException(label + ": glError " + error+ " " + GLU.gluErrorString(error));
        }
    }

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setConvertTapIntoTrigger(true);
        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        modelScreen = new float[16];
        camera = new float[16];
        Matrix.setIdentityM(camera, 0);
        sceneView = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        modelFloor = new float[16];
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.i(TAG, "onCreate");
    }

    private int loadScreenTexture() {
        final int[] textureHandle = new int[1];
 
        GLES20.glGenTextures(1, textureHandle, 0);
 
        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }
 
        return textureHandle[0];
    }

    private int loadPointerTexture() {
        final int[] textureHandle = new int[1];
 
        GLES20.glGenTextures(1, textureHandle, 0);
 
        if (textureHandle[0] != 0) {
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
 
            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D
                                   , GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D
                                   , GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
 
            ByteBuffer imageBuf = ByteBuffer.allocateDirect(4*1280*800)
                .order(ByteOrder.nativeOrder());
            byte[] bufArr = imageBuf.array();
            Arrays.fill(bufArr, (byte)0);
            imageBuf.position(0);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA
                                , 1280, 800, 0, GLES20.GL_RGBA
                                , GLES20.GL_UNSIGNED_BYTE, imageBuf);
        } else {
            throw new RuntimeException("Error loading texture.");
        }
 
        return textureHandle[0];
    }

    private void fillScreenCoords() {
        int stacks = 50;
        int slices = 50;
        float width = 16.0f;
        float height = 9.0f;
        float depth = -5.0f;

        screenVertices = ByteBuffer.allocateDirect(
            stacks * slices * COORDS_PER_VERTEX * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        screenTextures = ByteBuffer.allocateDirect(
            stacks * slices * COORDS_PER_TEXTURE * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        screenIndices = ByteBuffer.allocateDirect(
            (stacks - 1) * 2 * (slices + 2) * BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder()).asShortBuffer();

        float v_sector = (float)Math.PI / (stacks - 1);
        float h_sector = (float)Math.PI / (slices - 1);
        float[] ys = new float[stacks];
        float[] xzs = new float[slices * 2];
        float[] dws = new float[slices];
        for (int i = 0; i < stacks; i += 1) {
             ys[i] = (float)Math.cos(i * v_sector) * height;
        }
        float realw = 0f;
        float oldx = width;
        float oldz = 0f;
        for (int i = 0; i < slices; i += 1) {
            float x = (float)Math.cos(i * h_sector) * width;
            float z = (float)Math.sin(i * h_sector) * depth;
            xzs[i*2] = x;
            xzs[i*2 + 1] = z;
            float dw = (float)Math.sqrt((oldx - x)*(oldx - x) + (oldz - z)*(oldz - z));
            dws[i] = dw;
            realw += dw;
            oldx = x;
            oldz = z;
        }
        for (int i = 0; i < stacks; i += 1) {
            float t = 1 - .5f * (ys[i] + height) / height;
            float old_s = 1f;
            for (int j = 0; j < slices; j += 1) {
                screenVertices.put(xzs[j*2]);
                screenVertices.put(ys[i]);
                screenVertices.put(xzs[j*2 + 1]);
                float s = old_s - (dws[j]/realw);
                screenTextures.put(s);
                screenTextures.put(t);
                old_s = s;
            }
            if (i < stacks - 1) {
                if (i > 0) {
                    // Degenerate begin: repeat first vertex
                    screenIndices.put((short)(i * slices));
                }
                for (int j = 0; j < slices; j += 1) {
                    // One part of the strip
                    screenIndices.put((short)(i * slices + j));
                    screenIndices.put((short)((i + 1) * slices + j));
                }
                if (i < stacks - 2) {
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

    public void requestRender() {
        getCardboardView().requestRender();
    }
    
    /**
     * Creates the buffers we use to store information about the 3D world.
     *
     * OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

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

        screenTexDataHandle = loadScreenTexture();
        pointerTexDataHandle = loadPointerTexture();
        checkGLError("Screen program");

        screenPositionParam = GLES20.glGetAttribLocation(screenProgram, "a_Position");
        screenTexParam = GLES20.glGetAttribLocation(screenProgram, "a_TexCoord");

        screenModelViewProjectionParam = GLES20.glGetUniformLocation(screenProgram, "u_MVP");
        screenTexUnihandle = GLES20.glGetUniformLocation(screenProgram, "u_TexScreen");
        pointTexUnihandle = GLES20.glGetUniformLocation(screenProgram, "u_TexPointer");

        GLES20.glEnableVertexAttribArray(screenPositionParam);
        GLES20.glEnableVertexAttribArray(screenTexParam);

        checkGLError("Screen program params");

        floorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(floorProgram, floorVertexShader);
        GLES20.glAttachShader(floorProgram, gridShader);
        GLES20.glLinkProgram(floorProgram);
        GLES20.glUseProgram(floorProgram);

        checkGLError("Floor program");

        floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
        floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");
        floorColorUParam = GLES20.glGetUniformLocation(floorProgram, "u_Color");

        floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
        floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");

        GLES20.glEnableVertexAttribArray(floorPositionParam);
        GLES20.glEnableVertexAttribArray(floorNormalParam);

        checkGLError("Floor program params");

        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Object first appears directly in front of user.
        Matrix.setIdentityM(modelScreen, 0);
        Matrix.translateM(modelScreen, 0, 0, 0, -objectDistance);

        Matrix.setIdentityM(modelFloor, 0);
        Matrix.translateM(modelFloor, 0, 0, -FLOOR_DEPTH, 0); // Floor appears below user.

        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        checkGLError("onSurfaceCreated");
        imageQueue = new ArrayBlockingQueue<Update>(10);
        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        UsbAccessory accessory = (UsbAccessory) getIntent()
            .getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

        if (accessory == null) {
            UsbAccessory[] accessories = manager.getAccessoryList();
            if (accessories == null) {
                return;
            }
            int i = accessories.length;
            while (i > 0 && accessory == null) {
                i -= 1;
                //TODO: should be changed to url so other implementations are covered
                if ("x-viredero".equals(accessories[i].getModel())) {
                    accessory = accessories[i];
                }
            }
            if (accessory == null) {
                return;
            }
        }
        usbFd = manager.openAccessory(accessory);
        if (usbFd == null) {
            return;
        }
        Runnable r = new UsbCmdPump(imageQueue, this, screenTexDataHandle
                                    , pointerTexDataHandle, usbFd);
        cmdPump = new Thread(r);
        cmdPump.start();
    }

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        lastHeadXform = headTransform;
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("mColorParam");

// TODO: camera = identity -> eyeView = sceneView
        Matrix.multiplyMM(sceneView, 0, eye.getEyeView(), 0, camera, 0);

        Matrix.multiplyMV(lightPosInEyeSpace, 0, sceneView, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(modelView, 0, sceneView, 0, modelScreen, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        drawScreen();

        Matrix.multiplyMM(modelView, 0, sceneView, 0, modelFloor, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0,
                          modelView, 0);
        drawFloor();
    }

    /**
     * Draw the screen.
     *
     * We've set all of our transformation matrices. Now we simply pass them into the shader.
     */
    private void drawScreen() {
        GLES20.glUseProgram(screenProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, screenTexDataHandle);
        GLES20.glUniform1i(screenTexUnihandle, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pointerTexDataHandle);
        GLES20.glUniform1i(pointTexUnihandle, 1);
        Update update = imageQueue.poll();
        if (update != null) {
            update.draw();
        }
        
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
 
        checkGLError("Drawing screen");
    }

    public void drawFloor() {
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

        checkGLError("drawing floor");
    }

    @Override
    public void onCardboardTrigger() {
        float[] headView = new float[16];
        lastHeadXform.getHeadView(headView, 0);
        Matrix.invertM(modelScreen, 0, headView, 0);
        System.arraycopy(modelScreen, 0, modelFloor, 0, 16);
        Matrix.translateM(modelScreen, 0, 0, 0, -objectDistance);
        Matrix.translateM(modelFloor, 0, 0, -FLOOR_DEPTH, 0); // Floor appears below user.
        vibrator.vibrate(50);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");
        cmdPump.interrupt();
        while (cmdPump.isAlive()) {
            try {
                cmdPump.join();
            } catch (InterruptedException ie) {
            }
        }
        try {
            usbFd.close();
        } catch(IOException e) {
        }
        finish();
    }
    @Override
    public void onRendererShutdown() {
    }
    @Override
    public void onSurfaceChanged(int width, int height) {
    }
    @Override
    public void onFinishFrame(Viewport viewport) {
    }

}

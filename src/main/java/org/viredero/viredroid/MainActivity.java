/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.viredero.viredroid;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.util.FloatMath;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
/**
 * A Cardboard sample application.
 */
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

    private static final String TAG = "viredroid";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    private static final int COORDS_PER_VERTEX = 3;
    private static final int COORDS_PER_NORMAL = 3;
    private static final int COORDS_PER_COLOR = 4;
    private static final int COORDS_PER_TEXTURE = 2;
    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;
/** Store our model data in a float buffer. */
 
/** This will be used to pass in the texture. */
 
/** This will be used to pass in model texture coordinate information. */
    
    private static final WorldLayoutData DATA = new WorldLayoutData();

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] { 0.0f, 2.0f, 0.0f, 1.0f };

    private final float[] mLightPosInEyeSpace = new float[4];

    private FloatBuffer mFloorVertices;
    private FloatBuffer mFloorColors;
    private FloatBuffer mFloorNormals;

    private FloatBuffer mCubeVertices;
    private ShortBuffer mCubeIndices;
    private FloatBuffer mCubeColors;
    private FloatBuffer mCubeNormals;
    private FloatBuffer mCubeTextures;
    private int cubeIndicesSize;

    private int[] vbo = new int[1];
    private int[] ibo = new int[1];

    private int mCubeProgram;
    private int mFloorProgram;

    private int mCubePositionParam;
    private int mCubeNormalParam;
    private int mCubeColorParam;
    private int mCubeTexParam;
    private int mCubeModelParam;
    private int mCubeModelViewParam;
    private int mCubeModelViewProjectionParam;
    private int mCubeLightPosParam;
    private int mCubeTexUnihandle;
 
    private int mTextureDataHandle;

    private int mFloorPositionParam;
    private int mFloorNormalParam;
    private int mFloorColorParam;
    private int mFloorModelParam;
    private int mFloorModelViewParam;
    private int mFloorModelViewProjectionParam;
    private int mFloorLightPosParam;

    private float[] mModelCube;
    private float[] mCamera;
    private float[] mView;
    private float[] mHeadView;
    private float[] mModelViewProjection;
    private float[] mModelView;
    private float[] mModelFloor;

    private int mScore = 0;
    private float mObjectDistance = 10f;
    private float mFloorDepth = 20f;

    private Vibrator mVibrator;
    private ViredroidOverlayView mOverlayView;

    
    private int loadTxtShader(int type, String shaderCode){
        
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

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
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
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
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        mModelCube = new float[16];
        mCamera = new float[16];
        mView = new float[16];
        mModelViewProjection = new float[16];
        mModelView = new float[16];
        mModelFloor = new float[16];
        mHeadView = new float[16];
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);


        mOverlayView = (ViredroidOverlayView) findViewById(R.id.overlay);
        mOverlayView.show3DToast("Pull the magnet when you find an object.");
    }

    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    private int loadTexture(final int resourceId) {
        final int[] textureHandle = new int[1];
 
        GLES20.glGenTextures(1, textureHandle, 0);
 
        if (textureHandle[0] != 0) {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling
 
            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeResource(
                getResources(), resourceId, options);
 
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
 
            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D
                                   , GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D
                                   , GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
 
            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
 
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }
 
        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }
 
        return textureHandle[0];
    }

    private void fillCubeCoords() {
        int stacks = 50;
        int slices = 50;
        float width = 16.0f;
        float height = 9.0f;
        float depth = -5.0f;

        mCubeVertices = ByteBuffer.allocateDirect(
            stacks * slices * COORDS_PER_VERTEX * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeNormals = ByteBuffer.allocateDirect(
            stacks * slices * COORDS_PER_NORMAL * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeColors = ByteBuffer.allocateDirect(
            stacks * slices * COORDS_PER_COLOR * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextures = ByteBuffer.allocateDirect(
            stacks * slices * COORDS_PER_TEXTURE * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeIndices = ByteBuffer.allocateDirect(
            (stacks - 1) * 2 * (slices + 2) * BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder()).asShortBuffer();

        float v_sector = (float)Math.PI / (stacks - 1);
        float h_sector = (float)Math.PI / (slices - 1);
        // FIXME: coords have to be changed to array of x,z and array of y's
        //         and combined to mCubeVertices
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
                mCubeVertices.put(xzs[j*2]);
                mCubeVertices.put(ys[i]);
                mCubeVertices.put(xzs[j*2 + 1]);
                mCubeNormals.put(-xzs[j*2]);
                mCubeNormals.put(-ys[i]);
                mCubeNormals.put(-xzs[j*2 + 1]);
                mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
                float s = old_s - (dws[j]/realw);
                mCubeTextures.put(s);
                mCubeTextures.put(t);
                old_s = s;
            }
            if (i < stacks - 1) {
                if (i > 0) {
                    // Degenerate begin: repeat first vertex
                    mCubeIndices.put((short)(i * slices));
                }
                for (int j = 0; j < slices; j += 1) {
                    // One part of the strip
                    mCubeIndices.put((short)(i * slices + j));
                    mCubeIndices.put((short)((i + 1) * slices + j));
                }
                if (i < stacks - 2) {
                    // Degenerate end: repeat last vertex
                    mCubeIndices.put(mCubeIndices.get(mCubeIndices.position() - 1));
                }
            }
        }
        // for (int i = 0; i < stacks; i += 1) {
        //     float y = (float)Math.cos(i * v_sector) * height;
        //     float t = .5f * (y + height) / height;
        //     float oldx = width;
        //     float oldz = 0f;
        //     float olds = 1f;
        //     float realw = 0f;
        //     for (int j = 0; j < slices; j += 1) {
        //         float x = (float)Math.cos(j * h_sector) * width;
        //         float z = (float)Math.sin(j * h_sector) * depth;
        //         mCubeVertices.put(new float[]{x, y, z});
        //         mCubeNormals.put(new float[]{-x, -y, -z});
        //         mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
        //         float dw = Math.sqrt((oldx - x)*(oldx - x) + (oldz - z)*(oldz - z));
        //         realw += dw;
        //         mCubeTextures.put(new float[]{s, t});
	//     }
// I/viredroid(13454): !!!c 30.000000 21.000000 0.000000
// I/viredroid(13454): !!!c -0.000001 21.000000 0.000000
// I/viredroid(13454): !!!c -30.000000 21.000000 0.000000
// I/viredroid(13454): !!!c 30.000000 -21.000000 0.000000
// I/viredroid(13454): !!!c -0.000001 -21.000000 0.000000
// I/viredroid(13454): !!!c -30.000000 -21.000000 0.000000
// I/viredroid(13454): !!!i 0 3 1 4 2 5 

        cubeIndicesSize = mCubeIndices.position();
        mCubeIndices.position(0);
        mCubeVertices.position(0);
        mCubeNormals.position(0);
        mCubeColors.position(0);
        mCubeTextures.position(0);
    }   

    private void _fillCubeCoords() {
        mCubeVertices = ByteBuffer.allocateDirect(3 * COORDS_PER_VERTEX * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeNormals = ByteBuffer.allocateDirect(3 * COORDS_PER_NORMAL * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeColors = ByteBuffer.allocateDirect(3 * COORDS_PER_COLOR * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeIndices = ByteBuffer.allocateDirect(4 * BYTES_PER_SHORT)
            .order(ByteOrder.nativeOrder()).asShortBuffer();

        mCubeVertices.put(
            new float[]{-1.0f, 0f, 0f
                        , -.5f, 1.0f, 0f
                        , 0f, 0f, 0f}).position(0);
        mCubeNormals.put(
            new float[]{0f, 0f, 1f
                        , 0f, 0f, 1f
                        , 0f, 0f, 1f}).position(0);
        mCubeColors.put(
            new float[] {0f, 0.5273f, 0.2656f, 1.0f
                         , 0f, 0.5273f, 0.2656f, 1.0f
                         , 0f, 0.5273f, 0.2656f, 1.0f}).position(0);
        mCubeIndices.put(
            new short[]{0, 1, 2, 2}).position(0);
        // GLES20.glGenBuffers(1, vbo, 0);
        // GLES20.glGenBuffers(1, ibo, 0);
        // if (ibo[0] <= 0) {
        //     checkGLError("buffer generation");
        // }
        // GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
        // GLES20.glBufferData(
	//     GLES20.GL_ARRAY_BUFFER
	//     , 3 * (COORDS_PER_VERTEX + COORDS_PER_NORMAL + COORDS_PER_COLOR)
	//          * BYTES_PER_FLOAT
	//      , mCubeVertices, GLES20.GL_STATIC_DRAW);
 
        // GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
        // GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, 4 * BYTES_PER_SHORT
        //                     , mCubeIndices, GLES20.GL_STATIC_DRAW);
        
        // GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        // GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
        cubeIndicesSize = 3;
    }
    
    private void __fillCubeCoords() {
        // float radius = 1.0f;
        // float pi_stacks = (float)Math.PI / stacks;
        // float pi_slices = (float)Math.PI / slices;
        // for (int stackNumber = 0; stackNumber <= stacks; stackNumber += 1) {
        //     for (int sliceNumber = 0; sliceNumber < slices; sliceNumber += 1) {
        //         float theta = (float) (stackNumber * pi_stacks);
        //         float phi = (float) (sliceNumber * pi_slices);
        //         float sinTheta = FloatMath.sin(theta);
        //         float sinPhi = FloatMath.sin(phi);
        //         float cosTheta = FloatMath.cos(theta);
        //         float cosPhi = FloatMath.cos(phi);
        //      float x = radius * cosPhi * sinTheta;
        //      float y = radius * sinPhi * sinTheta;
        //      float z = radius * cosTheta;
        //      Log.i(TAG, "!!!c " + x + " " + y + " " + z);
        //      mCubeVertices.put(new float[]{x, y, z});
        //      mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
        //      mCubeNormals.put(new float[]{-x, -y, -z});
        //     }
        // }
      
        mCubeVertices.put(new float[]{-1.0f, 0f, 0f});
        mCubeVertices.put(new float[]{-.5f, 1.0f, 0f});
        mCubeVertices.put(new float[]{0f, 0f, 0f});
//      mCubeVertices.put(new float[]{.5f, 1f, 0f});
//      mCubeVertices.put(new float[]{1.0f, 0f, .5f});
        mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
        mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
        mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
//      mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
//      mCubeColors.put(new float[]{0f, 0.5273f, 0.2656f, 1.0f});
        mCubeNormals.put(new float[]{0f, 0f, 1f});
        mCubeNormals.put(new float[]{0f, 0f, 1f});
        mCubeNormals.put(new float[]{0f, 0f, 1f});
//      mCubeNormals.put(new float[]{0f, 0f, 1f});
//      mCubeNormals.put(new float[]{0f, 0f, 1f});
        mCubeIndices.put(new short[]{0, 1});
        mCubeIndices.put(new short[]{1, 2});
        mCubeIndices.put(new short[]{2, 3});
        mCubeIndices.put(new short[]{3, 4});

        // for (int stackNumber = 0; stackNumber < stacks; stackNumber += 1){
        //     for (int sliceNumber = 0; sliceNumber <= slices; sliceNumber += 1) {
        //      short i = (short) ((stackNumber * slices) + (sliceNumber % slices));
        //      short j = (short) (((stackNumber + 1) * slices)
        //                         + (sliceNumber % slices));
        //          Log.i(TAG, "!!!i " + i + " " + j);
        //         mCubeIndices.put(i);
        //         mCubeIndices.put(j);
        //     }
        // }
        mCubeVertices.position(0);
        mCubeColors.position(0);
        mCubeNormals.position(0);
        mCubeIndices.position(0);

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

        fillCubeCoords();
        // make a floor
        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(DATA.FLOOR_COORDS.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        mFloorVertices = bbFloorVertices.asFloatBuffer();
        mFloorVertices.put(DATA.FLOOR_COORDS);
        mFloorVertices.position(0);

        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(DATA.FLOOR_NORMALS.length * 4);
        bbFloorNormals.order(ByteOrder.nativeOrder());
        mFloorNormals = bbFloorNormals.asFloatBuffer();
        mFloorNormals.put(DATA.FLOOR_NORMALS);
        mFloorNormals.position(0);

        ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(DATA.FLOOR_COLORS.length * 4);
        bbFloorColors.order(ByteOrder.nativeOrder());
        mFloorColors = bbFloorColors.asFloatBuffer();
        mFloorColors.put(DATA.FLOOR_COLORS);
        mFloorColors.position(0);

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);
        mCubeProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mCubeProgram, vertexShader);
        GLES20.glAttachShader(mCubeProgram, passthroughShader);
        GLES20.glLinkProgram(mCubeProgram);
        GLES20.glUseProgram(mCubeProgram);

        mTextureDataHandle = loadTexture(R.drawable.test);
        checkGLError("Cube program");

        mCubePositionParam = GLES20.glGetAttribLocation(mCubeProgram, "a_Position");
        mCubeNormalParam = GLES20.glGetAttribLocation(mCubeProgram, "a_Normal");
        mCubeColorParam = GLES20.glGetAttribLocation(mCubeProgram, "a_Color");
        mCubeTexParam = GLES20.glGetAttribLocation(mCubeProgram, "a_TexCoord");

        mCubeModelParam = GLES20.glGetUniformLocation(mCubeProgram, "u_Model");
        mCubeModelViewParam = GLES20.glGetUniformLocation(mCubeProgram, "u_MVMatrix");
        mCubeModelViewProjectionParam = GLES20.glGetUniformLocation(mCubeProgram, "u_MVP");
        mCubeLightPosParam = GLES20.glGetUniformLocation(mCubeProgram, "u_LightPos");
        mCubeTexUnihandle = GLES20.glGetUniformLocation(mCubeProgram, "u_Texture");

        GLES20.glEnableVertexAttribArray(mCubePositionParam);
        GLES20.glEnableVertexAttribArray(mCubeNormalParam);
        GLES20.glEnableVertexAttribArray(mCubeColorParam);

        checkGLError("Cube program params");

        mFloorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mFloorProgram, vertexShader);
        GLES20.glAttachShader(mFloorProgram, gridShader);
        GLES20.glLinkProgram(mFloorProgram);
        GLES20.glUseProgram(mFloorProgram);

        checkGLError("Floor program");

        mFloorModelParam = GLES20.glGetUniformLocation(mFloorProgram, "u_Model");
        mFloorModelViewParam = GLES20.glGetUniformLocation(mFloorProgram, "u_MVMatrix");
        mFloorModelViewProjectionParam = GLES20.glGetUniformLocation(mFloorProgram, "u_MVP");
        mFloorLightPosParam = GLES20.glGetUniformLocation(mFloorProgram, "u_LightPos");

        mFloorPositionParam = GLES20.glGetAttribLocation(mFloorProgram, "a_Position");
        mFloorNormalParam = GLES20.glGetAttribLocation(mFloorProgram, "a_Normal");
        mFloorColorParam = GLES20.glGetAttribLocation(mFloorProgram, "a_Color");

        GLES20.glEnableVertexAttribArray(mFloorPositionParam);
        GLES20.glEnableVertexAttribArray(mFloorNormalParam);
        GLES20.glEnableVertexAttribArray(mFloorColorParam);

        checkGLError("Floor program params");

        
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Object first appears directly in front of user.
        Matrix.setIdentityM(mModelCube, 0);
        Matrix.translateM(mModelCube, 0, 0, 0, -mObjectDistance);

        Matrix.setIdentityM(mModelFloor, 0);
        Matrix.translateM(mModelFloor, 0, 0, -mFloorDepth, 0); // Floor appears below user.

        checkGLError("onSurfaceCreated");
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

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the Model part of the ModelView matrix.
        //        Matrix.rotateM(mModelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(mHeadView, 0);

        checkGLError("onReadyToDraw");
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("mColorParam");

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(mView, 0, eye.getEyeView(), 0, mCamera, 0);

        // Set the position of the light
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mView, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        Matrix.multiplyMM(mModelView, 0, mView, 0, mModelCube, 0);
        Matrix.multiplyMM(mModelViewProjection, 0, perspective, 0, mModelView, 0);
        //      sphere.draw(mModelViewProjection);
        drawCube();

        // Set mModelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(mModelView, 0, mView, 0, mModelFloor, 0);
        Matrix.multiplyMM(mModelViewProjection, 0, perspective, 0,
                          mModelView, 0);
        drawFloor();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    /**
     * Draw the cube.
     *
     * We've set all of our transformation matrices. Now we simply pass them into the shader.
     */
    public void drawCube() {
        GLES20.glUseProgram(mCubeProgram);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
 
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
 
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mCubeTexUnihandle, 0);// Draw
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0]);
 
// Bind Attributes
        // Set the position of the surface
        GLES20.glVertexAttribPointer(mCubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT
                                     , false, 0, mCubeVertices);

        // Set the normal positions of the surface, again for shading
        GLES20.glVertexAttribPointer(mCubeNormalParam, COORDS_PER_NORMAL, GLES20.GL_FLOAT
                                     , false, 0, mCubeNormals);

        GLES20.glVertexAttribPointer(mCubeColorParam, COORDS_PER_COLOR, GLES20.GL_FLOAT
                                     , false, 0, mCubeColors);
 
        GLES20.glVertexAttribPointer(mCubeTexParam, COORDS_PER_TEXTURE
                                     , GLES20.GL_FLOAT, false, 0, mCubeTextures);

        GLES20.glEnableVertexAttribArray(mCubeTexParam);

        GLES20.glUniform3fv(mCubeLightPosParam, 1, mLightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(mCubeModelParam, 1, false, mModelCube, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(mCubeModelViewParam, 1, false, mModelView, 0);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(mCubeModelViewProjectionParam, 1
                                  , false, mModelViewProjection, 0);

//        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP
			      , cubeIndicesSize
                              , GLES20.GL_UNSIGNED_SHORT, mCubeIndices);
 
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
//        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

        checkGLError("Drawing cube");
    }

    /**
     * Draw the floor.
     *
     * This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void drawFloor() {
        GLES20.glUseProgram(mFloorProgram);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(mFloorLightPosParam, 1, mLightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(mFloorModelParam, 1, false, mModelFloor, 0);
        GLES20.glUniformMatrix4fv(mFloorModelViewParam, 1, false, mModelView, 0);
        GLES20.glUniformMatrix4fv(mFloorModelViewProjectionParam, 1, false,
                                  mModelViewProjection, 0);
        GLES20.glVertexAttribPointer(mFloorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                                     false, 0, mFloorVertices);
        GLES20.glVertexAttribPointer(mFloorNormalParam, 3, GLES20.GL_FLOAT, false, 0,
                                     mFloorNormals);
        GLES20.glVertexAttribPointer(mFloorColorParam, 4, GLES20.GL_FLOAT, false, 0, mFloorColors);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        checkGLError("drawing floor");
    }

    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

        if (isLookingAtObject()) {
            mScore++;
            mOverlayView.show3DToast("Found it! Look around for another one.\nScore = " + mScore);
            hideObject();
        } else {
            mOverlayView.show3DToast("Look around to find the object!");
        }

        // Always give user feedback.
        mVibrator.vibrate(50);
    }

    /**
     * Find a new random position for the object.
     *
     * We'll rotate it around the Y-axis so it's out of sight, and then up or down by a little bit.
     */
    private void hideObject() {
        float[] rotationMatrix = new float[16];
        float[] posVec = new float[4];

        // First rotate in XZ plane, between 90 and 270 deg away, and scale so that we vary
        // the object's distance from the user.
        float angleXZ = (float) Math.random() * 180 + 90;
        Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);
        float oldObjectDistance = mObjectDistance;
        mObjectDistance = (float) Math.random() * 15 + 5;
        float objectScalingFactor = mObjectDistance / oldObjectDistance;
        Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor,
                      objectScalingFactor);
        Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, mModelCube, 12);

        // Now get the up or down angle, between -20 and 20 degrees.
        float angleY = (float) Math.random() * 80 - 40; // Angle in Y plane, between -40 and 40.
        angleY = (float) Math.toRadians(angleY);
        float newY = (float) Math.tan(angleY) * mObjectDistance;

        Matrix.setIdentityM(mModelCube, 0);
        Matrix.translateM(mModelCube, 0, posVec[0], newY, posVec[2]);
    }

    /**
     * Check if user is looking at object by calculating where the object is in eye-space.
     *
     * @return true if the user is looking at the object.
     */
    private boolean isLookingAtObject() {
        float[] initVec = { 0, 0, 0, 1.0f };
        float[] objPositionVec = new float[4];

        // Convert object space to camera space. Use the headView from onNewFrame.
        Matrix.multiplyMM(mModelView, 0, mHeadView, 0, mModelCube, 0);
        Matrix.multiplyMV(objPositionVec, 0, mModelView, 0, initVec, 0);

        float pitch = (float) Math.atan2(objPositionVec[1], -objPositionVec[2]);
        float yaw = (float) Math.atan2(objPositionVec[0], -objPositionVec[2]);

        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }
}

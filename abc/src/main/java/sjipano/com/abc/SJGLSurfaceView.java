package sjipano.com.abc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES10;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class SJGLSurfaceView extends GLSurfaceView {
    private static final String TAG = SJGLSurfaceView.class.getName();
    private Renderer mRenderer;

    private float mPosX, mPosY;
    private float mCurPosX, mCurPosY;

    private float yaw = 0; //左右滑动
    private float fov = 120; //视角
    private float pitch = 0; //上下滑动
    private int mode = 0;//1单指滑动 2双指滑动
    private float preDistance = 0.1f;

    private int mWidth = 1080, mHeight = 1720;
    private static String FRAGMENT_SHADER = "1";
    private static String VERTEX_SHADER = "1";

    private float mClickX, mClickY;//用于判断 是否是点击事件

    private int mMaxSize;

    public SJGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public SJGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @NotProguard
    public boolean setRenderer(Context context, int resId) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        //获取资源图片
        InputStream stream = getResources().openRawResource(resId);
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(stream, null, opt);
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeStream(stream);
            }
        } catch (OutOfMemoryError error) {
            return false;
        }

        if (bitmap != null) {
            Log.d(TAG, "size:" + bitmap.getByteCount() + "b ,--" + (double) bitmap.getByteCount() / 1024 / 1024 + "M --,width:"
                    + bitmap.getWidth() + "--,height:" + bitmap.getHeight());
        } else {
            return false;
        }
        mRenderer = new Renderer(context, bitmap);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        return true;
    }

    @NotProguard
    public boolean setRenderer(Context context, String path) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        //获取资源图片
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        System.gc();
        System.runFinalization();
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(stream, null, opt);
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeStream(stream);
            }
        } catch (Exception o) {
            return false;
        } catch (OutOfMemoryError error) {
            return false;
        }

        if (bitmap != null) {
            Log.d(TAG, "size:" + bitmap.getByteCount() + "b ,--" + (double) bitmap.getByteCount() / 1024 / 1024 + "M --,width:"
                    + bitmap.getWidth() + "--,height:" + bitmap.getHeight());
        } else {
            return false;
        }
        if (mRenderer != null) {
            mRenderer.setBitmap(bitmap);
        } else {
            mRenderer = new Renderer(context, bitmap);
            setRenderer(mRenderer);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
        return true;
    }

    public void onDestroy() {
        if (mRenderer != null) {
            mRenderer.destroy();
        }
    }

    private void init() {
        try {
            FRAGMENT_SHADER = readAsset("fragshader.frags", getContext());
            VERTEX_SHADER = readAsset("vertshader.vs", getContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setWidAndHei(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = 2;
                preDistance = getDistance(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_DOWN:
                mPosX = event.getX();
                mPosY = event.getY();
                mClickX = event.getX();
                mClickY = event.getY();
                mode = 1;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == 1) {
                    mCurPosX = event.getX();
                    mCurPosY = event.getY();
                    //左右滑动
                    synchronized (this) {
                        yaw = yaw + (mCurPosX - mPosX) / 12;
                        yaw = yaw % 360.f;
                        if (yaw < 0) {
                            yaw = yaw + 360.f;
                        }
                    }

                    mPosX = mCurPosX;

                    //上下滑动
                    synchronized (this) {
                        pitch = pitch + (mCurPosY - mPosY) / 15;
                    }

                    mPosY = mCurPosY;
                    synchronized (this) {
                        if (pitch > 89) {
                            pitch = 89;
                        }
                        if (pitch < -89) {
                            pitch = -89;
                        }
                    }
                } else if (mode == 2) {
                    try {
                        float distance = getDistance(event);
                        if (distance > 10f) {
                            float scale = preDistance / distance;
                            fov = (float) (fov + fov * (scale - 1) * 0.2);
                            preDistance = distance;
                        }
                    } catch (Exception ignored) {

                    }
                }
                if (fov > 120) {
                    fov = 120;
                }
                if (fov < 45) {
                    fov = 45;
                }
                refreshUI(fov, yaw, pitch);
                break;
            case MotionEvent.ACTION_UP:
                if (listener != null) {
                    listener.saveXYZ();
                }
                if (Math.abs(event.getX() - mClickX) <= 5 && Math.abs(event.getY() - mClickY) <= 5) {
                    if (listener != null) {
                        listener.justClick();
                    }
                }
                break;
        }
        return true;
    }

    private void refreshUI(float f, float y, float p) {
        if (listener != null) {
            listener.positionUpdate(f, y, p, mWidth, mHeight);
        }
        requestRender();
    }

    public void getGLESTextureLimitBelowLollipop() {
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        mMaxSize = maxSize[0];
    }

    public void getGLESTextureLimitEqualAboveLollipop() {
        EGL10 egl = (EGL10) EGLContext.getEGL();
        EGLDisplay dpy = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        int[] vers = new int[2];
        egl.eglInitialize(dpy, vers);
        int[] configAttr = {
                EGL10.EGL_COLOR_BUFFER_TYPE, EGL10.EGL_RGB_BUFFER,
                EGL10.EGL_LEVEL, 0,
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfig = new int[1];
        egl.eglChooseConfig(dpy, configAttr, configs, 1, numConfig);
        if (numConfig[0] == 0) {// TROUBLE! No config found.
        }
        EGLConfig config = configs[0];
        int[] surfAttr = {
                EGL10.EGL_WIDTH, 64,
                EGL10.EGL_HEIGHT, 64,
                EGL10.EGL_NONE
        };
        EGLSurface surf = egl.eglCreatePbufferSurface(dpy, config, surfAttr);
        final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;  // missing in EGL10
        int[] ctxAttrib = {
                EGL_CONTEXT_CLIENT_VERSION, 1,
                EGL10.EGL_NONE
        };
        EGLContext ctx = egl.eglCreateContext(dpy, config, EGL10.EGL_NO_CONTEXT, ctxAttrib);
        egl.eglMakeCurrent(dpy, surf, surf, ctx);
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        egl.eglMakeCurrent(dpy, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(dpy, surf);
        egl.eglDestroyContext(dpy, ctx);
        egl.eglTerminate(dpy);
        mMaxSize = maxSize[0];
    }

    private float getDistance(MotionEvent event) {
        float x = event.getX(1) - event.getX(0);
        float y = event.getY(1) - event.getY(0);
        return (float) Math.sqrt(x * x + y * y);//两点间的距离
    }

    public class Renderer implements GLSurfaceView.Renderer {

        private List<Float> mCoords3DList = new ArrayList<>();//球面坐标集合
        private final int IterationsNum = 5;

        private static final float _PI = 3.1415926f;

        private final FloatBuffer mVertexBuffer;
        private final float[] mViewMatrix = new float[9];

        private int mProgram;

        private int mPositionHandle;

        private int mViewMatHandle;
        private int mImgSizeHandle;
        private int mcoshfovHandle;
        private int mcschfovHandle;
        private int mdHandle;

        private int mTexHandle;
        private Bitmap mBitmap;

        private void initVertices() {

            // mCoords3DList.clear();

            a.genChildTri(
                    0.0f, 1.0f, 0.0f,
                    0.707f, 0.0f, -0.707f,
                    0.707f, 0.0f, 0.707f,
                    IterationsNum, mCoords3DList);
            a.genChildTri(
                    0.0f, 1.0f, 0.0f,
                    0.707f, 0.0f, 0.707f,
                    -0.707f, 0.0f, 0.707f,
                    IterationsNum, mCoords3DList);
            a.genChildTri(
                    0.0f, 1.0f, 0.0f,
                    -0.707f, 0.0f, 0.707f,
                    -0.707f, 0.0f, -0.707f,
                    IterationsNum, mCoords3DList);
            a.genChildTri(
                    0.0f, 1.0f, 0.0f,
                    -0.707f, 0.0f, -0.707f,
                    0.707f, 0.0f, -0.707f,
                    IterationsNum, mCoords3DList);
            a.genChildTri(
                    0.0f, -1.0f, -0.0f,
                    0.707f, 0.0f, -0.707f,
                    0.707f, 0.0f, 0.707f,
                    IterationsNum, mCoords3DList);
            a.genChildTri(
                    0.0f, -1.0f, -0.0f,
                    0.707f, 0.0f, 0.707f,
                    -0.707f, 0.0f, 0.707f,
                    IterationsNum, mCoords3DList);
            a.genChildTri(
                    0.0f, -1.0f, -0.0f,
                    -0.707f, 0.0f, 0.707f,
                    -0.707f, 0.0f, -0.707f,
                    IterationsNum, mCoords3DList);
            a.genChildTri(
                    0.0f, -1.0f, -0.0f,
                    -0.707f, 0.0f, -0.707f,
                    0.707f, 0.0f, -0.707f,
                    IterationsNum, mCoords3DList);
        }

        public void setBitmap(Bitmap bitmap) {
            if (bitmap != null) {
                this.mBitmap = bitmap;
            }
        }

        public Renderer(final Context context, Bitmap bitmap) {
            this.mBitmap = bitmap;
            initVertices();
            mVertexBuffer = ByteBuffer.allocateDirect(mCoords3DList.size() * Float.SIZE)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            for (Float f : mCoords3DList) {
                mVertexBuffer.put(f);
            }
            mVertexBuffer.position(0);
        }

        int loadShader(int type, String shaderCode) {
            int shader = GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);
            //            LogUtils.e(GLES20.glGetShaderInfoLog(shader));
            return shader;
        }

        @Override
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {

            //            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //                getGLESTextureLimitEqualAboveLollipop();
            //            } else {
            //                getGLESTextureLimitBelowLollipop();
            //            }
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            mProgram = GLES20.glCreateProgram();

            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);
            GLES20.glUseProgram(mProgram);

            GLES20.glEnable(GLES20.GL_DEPTH_TEST);

            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "position");

            // Get the uniform locations
            mViewMatHandle = GLES20.glGetUniformLocation(mProgram, "_viewmat");
            mImgSizeHandle = GLES20.glGetUniformLocation(mProgram, "_size");
            mdHandle = GLES20.glGetUniformLocation(mProgram, "_d");
            mcoshfovHandle = GLES20.glGetUniformLocation(mProgram, "_coshfov");
            mcschfovHandle = GLES20.glGetUniformLocation(mProgram, "_cschfov");

            GLES20.glEnableVertexAttribArray(mPositionHandle);
            GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 12, mVertexBuffer);

            int[] texNames = new int[1];
            GLES20.glGenTextures(1, texNames, 0);
            mTexHandle = texNames[0];
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexHandle);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            if (mBitmap != null && !mBitmap.isRecycled()) {
                //                if (mMaxSize == 4096) {
                //                    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, PicEdit.setImgSize(mBitmap, mMaxSize, mMaxSize / 2), 0);
                //                } else {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
                //                }
            }
        }

        @Override
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            mWidth = width;
            mHeight = height;
            GLES20.glViewport(0, 0, width, height);
        }

        @Override
        public void onDrawFrame(GL10 unused) {

            GLES20.glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            // _d
            float d = sji_calc_d(getFov());

            float hfov;
            hfov = 0.5f * getFov() * _PI / 180.0f;
            // _coshfov
            float coshfov = (float) Math.cos(hfov);
            // _cschfov
            float cschfov = 1.0f / ((float) Math.sin(hfov));

            // viewmat
            float rad_p, rad_y;
            float cp, sp, cy, sy;
            rad_p = getPitch() * _PI / 180.0f;
            rad_y = getYaw() * _PI / 180.f;
            cp = (float) Math.cos(rad_p);
            sp = (float) Math.sin(rad_p);
            cy = (float) Math.cos(rad_y);
            sy = (float) Math.sin(rad_y);

            mViewMatrix[0] = cy;
            mViewMatrix[1] = sy * sp;
            mViewMatrix[2] = sy * cp;
            mViewMatrix[3] = 0.f;
            mViewMatrix[4] = cp;
            mViewMatrix[5] = -sp;
            mViewMatrix[6] = -sy;
            mViewMatrix[7] = cy * sp;
            mViewMatrix[8] = cy * cp;

            GLES20.glUniformMatrix3fv(mViewMatHandle, 1, false, mViewMatrix, 0);    // ic_main_tab_setting rota matrix
            GLES20.glUniform2f(mImgSizeHandle, mWidth, mHeight);  // ic_main_tab_setting imgsize
            GLES20.glUniform1f(mdHandle, d);   // ic_main_tab_setting d
            GLES20.glUniform1f(mcoshfovHandle, coshfov);   // ic_main_tab_setting coshfov
            GLES20.glUniform1f(mcschfovHandle, cschfov);   // ic_main_tab_setting cschfov

            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mCoords3DList.size() / 3);
        }

        float sji_calc_d(float _fov) {
            return 0.5f + 0.5f * (float) Math.tanh(_fov * 0.0143 - 2.0);
        }

        public void destroy() {
            GLES20.glDeleteTextures(1, new int[]{mTexHandle}, 0);
            if (mBitmap != null && !mBitmap.isRecycled()) {
                mBitmap.recycle();
                mBitmap = null;
            }
            int[] texNames = {mTexHandle};
            GLES20.glDeleteTextures(1, texNames, 0);
            System.gc();
            System.runFinalization();
            if (listener != null) {
                listener = null;
            }
        }
    }

    public interface PicChangeListener {
        void positionUpdate(float fov,
                            float yaw, float pitch,
                            float width, float height);

        void justClick();

        void saveXYZ();
    }

    private PicChangeListener listener;

    public void setListener(PicChangeListener listener) {
        this.listener = listener;
    }

    public synchronized float getYaw() {
        return yaw;
    }

    public synchronized void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public synchronized float getFov() {
        return fov;
    }

    public synchronized void setFov(float fov) {
        this.fov = fov;
    }

    public synchronized float getPitch() {
        return pitch;
    }

    public synchronized void setPitch(float pitch) {
        this.pitch = pitch;
    }

    private String readAsset(String name, Context context) throws Exception {
        InputStream is = null;
        try {
            is = context.getAssets().open(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return readTextFromSDcard(is);
    }

    private String readTextFromSDcard(InputStream is) throws Exception {
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder buffer = new StringBuilder("");
        String str;
        while ((str = bufferedReader.readLine()) != null) {
            buffer.append(str);
            buffer.append("\n");
        }
        return buffer.toString();
    }
}

package fr.greweb.rnwebgl;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.SparseArray;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.content.Context.MODE_PRIVATE;
import static fr.greweb.rnwebgl.RNWebGL.*;

public class RNWebGLView extends GLSurfaceView implements GLSurfaceView.Renderer {
  private boolean onSurfaceCreateCalled = false;
  private int ctxId = -1;
  private ThemedReactContext reactContext;
  int widthImage , heightImage ;
  boolean snapShot = false ;


  public RNWebGLView(ThemedReactContext context) {
    super(context);
    reactContext = context;
    this.setZOrderMediaOverlay(true);
    setEGLContextClientVersion(2);
//  setZOrderOnTop(true);
    setEGLConfigChooser(8, 8, 8, 8, 16, 0);
    getHolder().setFormat(PixelFormat.RGB_888);
    setRenderer(this);

  }

  private static SparseArray<RNWebGLView> mGLViewMap = new SparseArray<>();
  private ConcurrentLinkedQueue<Runnable> mEventQueue = new ConcurrentLinkedQueue<>();




  public void onSurfaceCreated(GL10 unused, EGLConfig config) {
    EGL14.eglSurfaceAttrib(EGL14.eglGetCurrentDisplay(), EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),
            EGL14.EGL_SWAP_BEHAVIOR, EGL14.EGL_BUFFER_PRESERVED);

    final RNWebGLView glView = this;
    if (!onSurfaceCreateCalled) {
      // On JS thread, get JavaScriptCore context, create RNWebGL context, call JS callback
      final ReactContext reactContext = (ReactContext) getContext();
      reactContext.runOnJSQueueThread(new Runnable() {
        @Override
        public void run() {
          ctxId = RNWebGLContextCreate(reactContext.getJavaScriptContextHolder().get());
          mGLViewMap.put(ctxId, glView);
          WritableMap arg = Arguments.createMap();
          arg.putInt("ctxId", ctxId);
          reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "surfaceCreate", arg);
        }
      });
      onSurfaceCreateCalled = true;
    }
  }

  private void saveBitmap(Bitmap bitmap) {
    try {
      String now = String.valueOf(System.currentTimeMillis());
      String mPath = this.getContext().getCacheDir() + "/" + now + ".jpg";
      File imageFile = new File(mPath);
      FileOutputStream outputStream = new FileOutputStream(imageFile);
      int quality = 100;
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
      outputStream.flush();
      outputStream.close();
      WritableMap arg = Arguments.createMap();
      arg.putString("url", "file://" + imageFile.getCanonicalPath());
      reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "dataReturned", arg);
    } catch (Exception e) {
      Log.e("TAG", e.toString(), e);
    }
  }

  public Bitmap takeScreenshot(GL10 mGL) {
    final int mWidth = this.getWidth();
    final int mHeight = this.getHeight();
    IntBuffer ib = IntBuffer.allocate(mWidth * mHeight);
    IntBuffer ibt = IntBuffer.allocate(mWidth * mHeight);
    mGL.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib);

    // Convert upside down mirror-reversed image to right-side up normal image.
    for (int i = 0; i < mHeight; i++) {
      for (int j = 0; j < mWidth; j++) {
        ibt.put((mHeight - i - 1) * mWidth + j, ib.get(i * mWidth + j));
      }
    }
    Bitmap mBitmap = Bitmap.createBitmap(mWidth, mHeight,Bitmap.Config.ARGB_8888);
    mBitmap.copyPixelsFromBuffer(ibt);
    mBitmap = getResizedBitmap(mBitmap, mWidth, mHeight);

    return mBitmap;
  }

  public Bitmap getResizedBitmap(Bitmap bm, int width, int height) {
    float scaleWidth = ((float) widthImage) / width;
    float scaleHeight = ((float) heightImage) / height;
    Matrix matrix = new Matrix();
    matrix.postScale(scaleWidth, scaleHeight);
    Bitmap resizedBitmap = Bitmap.createBitmap(
            bm, 0, 0, width, height, matrix, false);
    bm.recycle();
    return resizedBitmap;
  }

  public synchronized void onDrawFrame(GL10 unused) {
    // Flush any queued events
    for (Runnable r : mEventQueue) {
      r.run();
    }
    mEventQueue.clear();

    // ctxId may be unset if we get here (on the GL thread) before RNWebGLContextCreate(...) is
    // called on the JS thread to create the RNWebGL context and save its id (see above in
    // the implementation of `onSurfaceCreated(...)`)
    if (ctxId > 0) {
      RNWebGLContextFlush(ctxId);
    }

    if (snapShot) {
      saveBitmap(takeScreenshot(unused));
      snapShot = false;
    }
  }

  public void onSurfaceChanged(GL10 unused, int width, int height) {
  }

  public void onDetachedFromWindow() {
    mGLViewMap.remove(ctxId);
    reactContext.getNativeModule(RNWebGLTextureLoader.class).unloadWithCtxId(ctxId);
    RNWebGLContextDestroy(ctxId);
    super.onDetachedFromWindow();
  }

  public synchronized void runOnGLThread(Runnable r) {
    mEventQueue.add(r);
  }

  public synchronized static void runOnGLThread(int ctxId, Runnable r) {
    RNWebGLView glView = mGLViewMap.get(ctxId);
    if (glView != null) {
      glView.runOnGLThread(r);
    }
  }



}

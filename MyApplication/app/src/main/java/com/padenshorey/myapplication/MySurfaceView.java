package com.padenshorey.myapplication;

import java.io.IOException;
import java.nio.Buffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class MySurfaceView extends SurfaceView implements Callback,
        Camera.PreviewCallback {

    private SurfaceHolder mHolder;

    private Camera mCamera;
    private boolean isPreviewRunning = false;
    private byte [] rgbbuffer = new byte[256 * 256];
    private int [] rgbints = new int[256 * 256];

    protected final Paint rectanglePaint = new Paint();

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        rectanglePaint.setARGB(100, 200, 0, 0);
        rectanglePaint.setStyle(Paint.Style.FILL);
        rectanglePaint.setStrokeWidth(2);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(new Rect((int) Math.random() * 100,
                (int) Math.random() * 100, 200, 200), rectanglePaint);
        Log.w(this.getClass().getName(), "On Draw Called");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (this) {
            this.setWillNotDraw(false); // This allows us to make our own draw
            // calls to this canvas

            mCamera = Camera.open();

            Camera.Parameters p = mCamera.getParameters();
            p.setPreviewSize(240, 160);
            mCamera.setParameters(p);


            //try { mCamera.setPreviewDisplay(holder); } catch (IOException e)
            //  { Log.e("Camera", "mCamera.setPreviewDisplay(holder);"); }

            mCamera.startPreview();
            mCamera.setPreviewCallback(this);

        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (this) {
            try {
                if (mCamera != null) {
                    mCamera.stopPreview();
                    isPreviewRunning = false;
                    mCamera.release();
                }
            } catch (Exception e) {
                Log.e("Camera", e.getMessage());
            }
        }
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("Camera", "Got a camera frame");

        Canvas c = null;

        if(mHolder == null){
            return;
        }

        try {
            synchronized (mHolder) {
                c = mHolder.lockCanvas(null);

                // Do your drawing here
                // So this data value you're getting back is formatted in YUV format and you can't do much
                // with it until you convert it to rgb
                int bwCounter=0;
                int yuvsCounter=0;
                for (int y=0;y<160;y++) {
                    System.arraycopy(data, yuvsCounter, rgbbuffer, bwCounter, 240);
                    yuvsCounter=yuvsCounter+240;
                    bwCounter=bwCounter+256;
                }

                for(int i = 0; i < rgbints.length; i++){
                    rgbints[i] = (int)rgbbuffer[i];
                }

                //decodeYUV(rgbbuffer, data, 100, 100);
                c.drawBitmap(rgbints, 50, 256, 0, 0, 256, 256, false, new Paint());

                Log.d("SOMETHING", "Got Bitmap");

            }
        } finally {
            // do this in a finally so that if an exception is thrown
            // during the above, we don't leave the Surface in an
            // inconsistent state
            if (c != null) {
                mHolder.unlockCanvasAndPost(c);
            }
        }
    }
}
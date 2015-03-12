package com.padenshorey.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Created by padenshorey on 15-03-05.
 */
public class ArDisplayView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    public static final String DEBUG_TAG = "ArDisplayView Log";
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private Activity mActivity;

    private Camera.Parameters parameters;
    private Camera.Size previewSize;
    private int[] pixels;

    public Bitmap bmp;

    public ArDisplayView(Context context, Activity activity) {
        super(context);

        mActivity = activity;
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        mCamera.setDisplayOrientation((info.orientation - degrees + 360) % 360);

        try {
            mCamera.setPreviewDisplay(mHolder);
            parameters = mCamera.getParameters();
            previewSize = parameters.getPreviewSize();
            pixels = new int[previewSize.width * previewSize.height];
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "surfaceCreated exception: ", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        parameters = mCamera.getParameters();
        List<Camera.Size> prevSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size s : prevSizes)
        {
            if((s.height <= height) && (s.width <= width))
            {
                parameters.setPreviewSize(s.width, s.height);
                break;
            }
        }

        //parameters.setColorEffect(android.hardware.Camera.Parameters.EFFECT_MONO);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {

            public void onPreviewFrame(byte[] data, Camera camera) {
                //transforms NV21 pixel data into RGB pixels
                decodeYUV420SP(pixels, data, previewSize.width, previewSize.height);
                //Output the value of the top left pixel in the preview to LogCat

                //Log.d("Pixels", "The top right pixel has the following RGB (hexadecimal) values:"
                //        +Integer.toHexString(pixels[0]));

                //processImage(pixels, data);
            }

        });
    }

    private void processImage(int[] p_pixels, byte[] p_data){

        for(int i=0; i<p_pixels.length; i++) {
            String myColorString = Integer.toHexString(p_pixels[i]);
            int color = (int) Long.parseLong(myColorString, 16);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = (color >> 0) & 0xFF;

            float colorAverage = (r + g + b) / 3;

            if (colorAverage > (255 / 2)) {
                //Log.d("Pixels", "White");
                p_data [i] = (byte)16777215;
            } else {
                //Log.d("Pixels", "Black");
                p_data[i] = (byte)0;
            }
        }
        //Log.d("Pixels", "Black: " + black + ", White: " + white);

        int nrOfPixels = p_data.length / 3; // Three bytes per pixel.
        int pixels[] = new int[nrOfPixels];
        for(int i = 0; i < nrOfPixels; i++) {
            int r = p_data[3*i];
            int g = p_data[3*i + 1];
            int b = p_data[3*i + 2];
            pixels[i] = Color.rgb(r, g, b);
        }

        //bmp = Bitmap.createBitmap(pixels, 100, 100, Bitmap.Config.ARGB_8888);
        //Log.d("Pixels", "Bitmap: " + bmp);

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }


    //Method from Ketai project! Not mine! See below...
    void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {

        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)                  r = 0;               else if (r > 262143)
                    r = 262143;
                if (g < 0)                  g = 0;               else if (g > 262143)
                    g = 262143;
                if (b < 0)                  b = 0;               else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }
}

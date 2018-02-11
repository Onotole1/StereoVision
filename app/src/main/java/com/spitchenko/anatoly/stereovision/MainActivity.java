package com.spitchenko.anatoly.stereovision;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends AppCompatActivity implements VideoService.VideoListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialize success");
        } else {
            Log.i(TAG, "OpenCV initialize failed");
        }
    }

    boolean mBound = false;
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private final ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(final ComponentName className,
                                       final IBinder service) {
            if (className.getClassName().equals(VideoService.class.getName())) {
                ((VideoService.VideoServiceBinder) service).subscribe(MainActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName arg0) {
            mBound = false;
        }
    };
    private ImageView mImageView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImageView = findViewById(R.id.imageView);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final Intent intent = new Intent(this, VideoService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onDepthMapCreated(@NonNull final Mat depthMap) {
        final Bitmap bm = Bitmap.createBitmap(depthMap.cols(), depthMap.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(depthMap, bm);
        mImageView.setImageBitmap(bm);
    }
}

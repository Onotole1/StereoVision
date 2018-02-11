package com.spitchenko.anatoly.stereovision;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.opencv.calib3d.StereoBM;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

/**
 * Date: 08.02.18
 * Time: 22:46
 *
 * @author anatoly
 */

public class VideoService extends Service {

    private static final double FOCUS_LENGTH_PX = 113.3858267717d;
    private static final double BASE_LENGTH_PX = 377.9527559055d;
    private final Mat img1 = new Mat();
    private final Mat img2 = new Mat();
    private final VideoServiceBinder mVideoServiceBinder = new VideoServiceBinder();
    private VideoCapture videoCapture1;
    private VideoCapture videoCapture2;
    private volatile boolean isDepthRun = false;

    final Runnable depthRunnable = new Runnable() {
        @Override
        public void run() {

            videoCapture1 = new VideoCapture(0);
            videoCapture2 = new VideoCapture(1);

            while (isDepthRun) {

                videoCapture1.read(img1);

                videoCapture2.read(img2);


                Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2GRAY);

                Imgproc.cvtColor(img2, img2, Imgproc.COLOR_BGR2GRAY);

                final Mat disparity = new Mat();

                final StereoBM stereoBM = StereoBM.create();

                stereoBM.compute(img1, img2, disparity);

                mVideoServiceBinder.notifyOnDepthMapCreated(disparity);

                /*final Mat normalized = new Mat();

                Core.normalize(disparity, normalized, 0, 255, Core.NORM_MINMAX);

                final double[][][] distances = new double[normalized.rows()][normalized.cols()][1];

                for (int i = 0, width = normalized.cols(); i < width; i++) {
                    for (int j = 0, height = normalized.rows(); j < height; j++) {

                        final double distancePx = getDistancePx(normalized.get(j, i)[0]);

                        maxValue = distancePx > maxValue ? maxValue = distancePx : maxValue;

                        distances[j][i][0] = distancePx;
                    }
                }*/
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {

        if (!isDepthRun) {

            isDepthRun = true;

            new Thread(depthRunnable).start();

        }

        return mVideoServiceBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isDepthRun = false;

        mVideoServiceBinder.unSubscribe();

        if (videoCapture1 != null && videoCapture2 != null) {
            videoCapture1.release();
            videoCapture2.release();
        }
    }

    private double getDistancePx(final double disparity) {
        if (disparity == 0) {
            return 0;
        }

        return FOCUS_LENGTH_PX * BASE_LENGTH_PX / disparity;
    }

    interface VideoListener {
        void onDepthMapCreated(@NonNull Mat depthMap);
    }

    class VideoServiceBinder extends Binder {

        private VideoListener mVideoListener;

        void subscribe(@NonNull final VideoListener videoListener) {
            mVideoListener = videoListener;
        }

        void unSubscribe() {
            mVideoListener = null;
        }

        void notifyOnDepthMapCreated(@NonNull final Mat depthMap) {
            if (mVideoListener != null) {
                mVideoListener.onDepthMapCreated(depthMap);
            }
        }
    }
}

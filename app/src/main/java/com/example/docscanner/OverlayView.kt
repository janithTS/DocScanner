package com.example.docscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.opencv.core.Point;

import java.util.Arrays;
import java.util.List;

public class OverlayView extends SurfaceView implements SurfaceHolder.Callback {

    private int srcImageWidth;
    private int srcImageHeight;
    private SurfaceHolder surfaceHolder = null;
    private List<Point> points = null;
    private Paint stroke = new Paint();

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        Log.d("DDN", "initialize overlay view");
        srcImageWidth = 0;
        srcImageHeight = 0;
        if (surfaceHolder == null) {
            // Get surfaceHolder object.
            surfaceHolder = getHolder();
            // Add this as surfaceHolder callback object.
            surfaceHolder.addCallback(this);
        }
        stroke.setColor(Color.GREEN);
        // Set the parent view background color. This can not set surfaceview background color.
        this.setBackgroundColor(Color.TRANSPARENT);

        // Set current surfaceview at top of the view tree.
        this.setZOrderOnTop(true);

        this.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d("DDN", "surface created");
        if (points != null) {
            drawPolygon();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    public SurfaceHolder getSurfaceHolder() {
        return surfaceHolder;
    }

    public void setStroke(Paint paint) {
        stroke = paint;
    }

    public Paint getStroke() {
        return stroke;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
        drawPolygon();
    }

    public void setPointsAndImageGeometry(List<Point> points, int width, int height) {
        this.srcImageWidth = width;
        this.srcImageHeight = height;
        this.points = points;
        drawPolygon();
    }

    public void setSrcImageWidth(int width) {
        Log.d("DDN", "set image width: " + width);
        this.srcImageWidth = width;
    }

    public void setSrcImageHeight(int height) {
        Log.d("DDN", "set image height: " + height);
        this.srcImageHeight = height;
    }

    public int getSrcImageWidth() {
        return srcImageWidth;
    }

    public int getSrcImageHeight() {
        return srcImageHeight;
    }

    public void drawPolygon() {
        Log.d("DDN", "draw polygon");
        // Get and lock canvas object from surfaceHolder.
        Canvas canvas = surfaceHolder.lockCanvas();
        if (canvas == null) {
            Log.d("DDN", "canvas is null");
            return;
        }
        List<Point> pts;
        Log.d("DDN", "srcImageHeight: " + srcImageHeight);
        if (srcImageWidth != 0 && srcImageHeight != 0) {
            Log.d("DDN", "convert points");
            pts = Arrays.asList(convertPoints(canvas.getWidth(), canvas.getHeight()));
        } else {
            pts = points;
        }
        // Clear canvas
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        for (int index = 0; index <= pts.size() - 1; index++) {
            if (index == pts.size() - 1) {
                canvas.drawLine((float) pts.get(index).x, (float) pts.get(index).y, (float) pts.get(0).x, (float) pts.get(0).y, stroke);
            } else {
                canvas.drawLine((float) pts.get(index).x, (float) pts.get(index).y, (float) pts.get(index + 1).x, (float) pts.get(index + 1).y, stroke);
            }
        }

        // Unlock the canvas object and post the new draw.
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    public Point[] convertPoints(int canvasWidth, int canvasHeight) {
        Point[] newPoints = new Point[points.size()];
        double ratioX = ((double) canvasWidth) / srcImageWidth;
        double ratioY = ((double) canvasHeight) / srcImageHeight;
        for (int index = 0; index <= points.size() - 1; index++) {
            Point p = new Point();
            p.x = (int) (ratioX * points.get(index).x);
            p.y = (int) (ratioY * points.get(index).y);
            newPoints[index] = p;
        }
        return newPoints;
    }

}
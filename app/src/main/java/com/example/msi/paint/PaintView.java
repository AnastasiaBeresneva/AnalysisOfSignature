package com.example.msi.paint;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.text.GetChars;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class PaintView extends View {

    public static int BRUSH_SIZE = 20;
    public static final int DEFAULT_COLOR = Color.RED;
    public static final int DEFAULT_BG_COLOR = Color.WHITE;
    private static final float TOUCH_TOLERANCE = 4;
    private float mX, mY;
    private Path mPath;
    private Paint mPaint;
    private ArrayList<FingerPath> paths = new ArrayList<>();
    private int currentColor;
    private int backgroundColor = Color.GRAY;//DEFAULT_BG_COLOR;
    private int strokeWidth;
    private boolean emboss;
    private boolean blur;
    private MaskFilter mEmboss;
    private MaskFilter mBlur;
    private Bitmap mBitmap;
    private Paint mBitmapPaint = new Paint(Paint.DITHER_FLAG);
    private Canvas mCanvas;
    private PointF mCurrentPoint; //for drawing
    private Paint mSignaturePaint;
    private Paint mBackgroundPaint;
    private List<PointF> mSignatureControlPoints = new ArrayList<>();
    private List<PointF> mSignatureActionUpPoints = new ArrayList<>();

    private Bitmap mSignature;


    private int mTouchCounter;

    private ArrayList<Long> TimePeriodOnTouch = new ArrayList<>(); //on touch time measure
    private long timeStart;
    private long periodOnTouch;

    // Introduce element-based design
    private SignatureElement mCurrentElement;
    private ArrayList<SignatureElement> mSignatureElements;
    private long initialTimeValue;
    private boolean mStarted;
    //

    public PaintView(Context context) {
        this(context, null);
    }

    public PaintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(DEFAULT_COLOR);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setAlpha(0xff);

        mEmboss = new EmbossMaskFilter(new float[] {1, 1, 1}, 0.4f, 6, 3.5f);
        mBlur = new BlurMaskFilter(5, BlurMaskFilter.Blur.NORMAL);

        //
        mSignatureElements = new ArrayList<>();
        mStarted = false;
        //

    }

    public void init(DisplayMetrics metrics) {
        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);

        currentColor = DEFAULT_COLOR;
        strokeWidth = BRUSH_SIZE;
    }

    public void normal() {
        emboss = false;
        blur = false;
    }

    public void emboss() {
        emboss = true;
        blur = false;
    }

    public void blur() {
        emboss = false;
        blur = true;
    }

    public void clear() {
        //
        initialTimeValue = 0;
        mStarted = true;
        mSignatureElements = new ArrayList<>();
        mCurrentElement = null;
        //
        backgroundColor = DEFAULT_BG_COLOR;
        paths.clear();
        normal();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mStarted) {
            canvas.save();
            mCanvas.drawColor(backgroundColor);
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.restore();
        }
        else {
            canvas.save();
            mCanvas.drawColor(backgroundColor);

            for (FingerPath fp : paths) {
                mPaint.setColor(fp.color);
                mPaint.setStrokeWidth(fp.strokeWidth);
                mPaint.setMaskFilter(null);

                if (fp.emboss)
                    mPaint.setMaskFilter(mEmboss);
                else if (fp.blur)
                    mPaint.setMaskFilter(mBlur);

                mCanvas.drawPath(fp.path, mPaint);

            }

            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.restore();
        }
    }

    private void touchStart(float x, float y) {
        mPath = new Path();
        FingerPath fp = new FingerPath(currentColor, emboss, blur, strokeWidth, mPath);
        paths.add(fp);

        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
    }

    private void touchUp() {
        mPath.lineTo(mX, mY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // check if user click to New Signature button
        if (!mStarted) return true;

        PointF curPoint = new PointF(event.getX(), event.getY());
        String action = "";
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:

                // check if signature recording is starting
                if (initialTimeValue == 0){
                    initialTimeValue = System.currentTimeMillis();
                }

                // Start new signature element
                mCurrentElement = new SignatureElement(System.currentTimeMillis());
                mCurrentElement.AddPoint(curPoint, System.currentTimeMillis());
                //

                timeStart = System.currentTimeMillis();
                action = "ACTION_DOWN";
                mTouchCounter += 1;
                mCurrentPoint = curPoint;
                touchStart(x, y);
                invalidate();
                mSignatureControlPoints.add(mCurrentPoint);
                if (mCurrentPoint != null) {invalidate();}
                break;

            case MotionEvent.ACTION_UP:

                // add current element to the list
                if(mCurrentElement != null) {
                    mSignatureElements.add(mCurrentElement);
                    mCurrentElement = null;
                }
                else{
                    Log.i("ERROR:","Current element is null!");
                }
                //

                periodOnTouch = System.currentTimeMillis() - timeStart;
                timeStart = 0;
                TimePeriodOnTouch.add(periodOnTouch);
                action = "ACTION_UP";
                mSignatureActionUpPoints.add(curPoint);
                mCurrentPoint = null;
                touchUp();
                invalidate();
                Log.i("ON_TOUCH: ", String.valueOf(periodOnTouch));
                break;

            case MotionEvent.ACTION_MOVE:

                // record new point
                mCurrentElement.AddPoint(curPoint, System.currentTimeMillis());
                //

                mCurrentPoint = curPoint;
                mSignatureControlPoints.add(mCurrentPoint);

                if (mCurrentPoint != null)
                {
                    touchMove(x, y);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_CANCEL:

                // delete current element
                Log.i("INFO:","Current element was canceled!");
                if(mCurrentElement != null) {
                    mCurrentElement = null;
                }
                else{
                    Log.i("ERROR:","Current element is null!");
                }
                //

                periodOnTouch = System.currentTimeMillis() - timeStart;
                timeStart = 0;
                TimePeriodOnTouch.add(periodOnTouch);
                action = "ACTION_CANCEL";
                mCurrentPoint = null;
                break;
        }



        Log.i("CANVAS_VIEW: ", action + " at x=" + curPoint.x + ", y=" + curPoint.y);
        Log.i("ON_TOUCH_MASSIV: ", TimePeriodOnTouch.toString());
        return true;
    }

    public void WriteSignatureToFile() {
        if (mStarted){
            try{
                String fileName = "signature_" + initialTimeValue + ".csv";
                String header = "element_time, point_time, x, y\n";
                String data = header;
                for(SignatureElement element : mSignatureElements){
                    data += element.toString(initialTimeValue);
                }
                data += "\n\n";

                Context context = getContext();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(fileName, context.MODE_PRIVATE));
                outputStreamWriter.write(data);
                outputStreamWriter.close();
            }
            catch(IOException e){
                Log.e("EXCEPTION:", "File write failed: " + e.toString());
            }
        }
        else{
            Log.e("INFO:", "Nothing to save! Please crete new signature.");
        }
    }
}
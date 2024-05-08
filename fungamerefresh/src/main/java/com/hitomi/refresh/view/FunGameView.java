package com.hitomi.refresh.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.hitomi.refresh.R;

/**
 * Created by Hitomis on 2016/3/9.
 * email:196425254@qq.com
 */
abstract class FunGameView extends View {

    static final int STATUS_GAME_PREPAR = 0;

    static final int STATUS_GAME_PLAY = 1;

    static final int STATUS_GAME_OVER = 2;

    static final int STATUS_GAME_FINISHED = 3;

    /**
     * Default width size of dividing line
     */
    static final float DIVIDING_LINE_SIZE = 1.f;

    /**
     * Ratio of view height to screen height
     */
    static final float VIEW_HEIGHT_RATIO = .161f;

    private String textGameOver;
    private String textLoading;
    private String textLoadingFinished;

    protected Paint mPaint;

    protected TextPaint textPaint;

    protected float controllerPosition;

    protected int controllerSize;

    protected int screenWidth, screenHeight;

    protected int status = STATUS_GAME_PREPAR;

    protected int lModelColor, rModelColor, mModelColor;

    public FunGameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FunGame);
        lModelColor = typedArray.getColor(R.styleable.FunGame_left_model_color, Color.rgb(0, 0, 0));
        mModelColor = typedArray.getColor(R.styleable.FunGame_middle_model_color, Color.BLACK);
        rModelColor = typedArray.getColor(R.styleable.FunGame_right_model_color, Color.parseColor("#A5A5A5"));
        typedArray.recycle();

        initBaseTools();
        initBaseConfigParams(context);
        initConcreteView();
    }

    protected void initBaseTools() {
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#C1C2C2"));

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStrokeWidth(1.f);
    }

    protected void initBaseConfigParams(Context context) {
        controllerPosition = DIVIDING_LINE_SIZE;

        screenWidth = getScreenMetrics(context).widthPixels;
        screenHeight = getScreenMetrics(context).heightPixels;
    }

    protected abstract void initConcreteView();

    protected abstract void drawGame(Canvas canvas);

    protected abstract void resetConfigParams();

    /**
     * Draw dividing line
     * @param canvas default canvas
     */
    private void drawBoundary(Canvas canvas) {
        mPaint.setColor(Color.parseColor("#606060"));
        canvas.drawLine(0, 0, screenWidth, 0, mPaint);
        canvas.drawLine(0, getMeasuredHeight(), screenWidth, getMeasuredHeight(), mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(screenWidth, (int) (screenHeight * VIEW_HEIGHT_RATIO));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawBoundary(canvas);
        drawText(canvas);
        drawGame(canvas);
    }

    /**
     * Draw text content
     * @param canvas default canvas
     */
    private void drawText(Canvas canvas) {
        switch (status) {
            case STATUS_GAME_PREPAR:
            case STATUS_GAME_PLAY:
                textPaint.setTextSize(50);
                promptText(canvas, textLoading);
                break;
            case STATUS_GAME_FINISHED:
                textPaint.setTextSize(40);
                promptText(canvas, textLoadingFinished);
                break;
            case STATUS_GAME_OVER:
                textPaint.setTextSize(50);
                promptText(canvas, textGameOver);
                break;
        }
    }

    /**
     * Prompt text information
     * @param canvas default canvas
     * @param text relevant text string
     */
    private void promptText(Canvas canvas, String text) {
        float textX = (canvas.getWidth() - textPaint.measureText(text)) * .5f;
        float textY = canvas.getHeight()  * .5f - (textPaint.ascent() + textPaint.descent()) * .5f;
        canvas.drawText(text, textX, textY, textPaint);
    }


    /**
     * Move controller (controller object is the right image model in the specific control)
     * @param distance distance moved
     */
    public void moveController(float distance) {
        float maxDistance = (getMeasuredHeight() -  2 * DIVIDING_LINE_SIZE - controllerSize);

        if (distance > maxDistance) {
            distance = maxDistance;
        }

        controllerPosition = distance;
        postInvalidate();
    }

    /**
     * Move controller to starting point
     * @param duration duration
     */
    public void moveController2StartPoint(long duration) {
        ValueAnimator moveAnimator = ValueAnimator.ofFloat(controllerPosition, DIVIDING_LINE_SIZE);
        moveAnimator.setDuration(duration);
        moveAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                controllerPosition = Float.parseFloat(animation.getAnimatedValue().toString());
                postInvalidate();
            }
        });
        moveAnimator.start();
    }

    /**
     * Update current control status
     * @param status status code
     */
    public void postStatus(int status) {
        this.status = status;

        if (status == STATUS_GAME_PREPAR) {
            resetConfigParams();
        }

        postInvalidate();
    }

    /**
     * Get current control status
     * @return
     */
    public int getCurrStatus() {
        return status;
    }

    public String getTextGameOver() {
        return textGameOver;
    }

    public void setTextGameOver(String textGameOver) {
        this.textGameOver = textGameOver;
    }

    public String getTextLoading() {
        return textLoading;
    }

    public void setTextLoading(String textLoading) {
        this.textLoading = textLoading;
    }

    public String getTextLoadingFinished() {
        return textLoadingFinished;
    }

    public void setTextLoadingFinished(String textLoadingFinished) {
        this.textLoadingFinished = textLoadingFinished;
    }

    /**
     * Get screen size
     *
     * @param context context
     * @return mobile screen size
     */
    private DisplayMetrics getScreenMetrics(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(dm);
        return dm;
    }
}

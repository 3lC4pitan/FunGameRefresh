package com.hitomi.refresh.view;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;


import com.hitomi.refresh.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hitomis on 2016/2/29.
 * email:196425254@qq.com
 */
public class HitBlockView extends FunGameView {

/**
     * Default number of rectangular blocks arranged vertically
     */
    private static final int BLOCK_VERTICAL_NUM = 5;

    /**
     * Default number of rectangular blocks arranged horizontally
     */
    private static final int BLOCK_HORIZONTAL_NUM = 3;

    /**
     * Height of the rectangular block as a ratio of the screen height
     */
    private static final float BLOCK_HEIGHT_RATIO = .03125f;

    /**
     * Width of the rectangular block as a ratio of the screen width
     */
    private static final float BLOCK_WIDTH_RATIO = .01806f;

    /**
     * Position of the racket as a ratio of the screen width
     */
    private static final float RACKET_POSITION_RATIO = .8f;

    /**
     * Position of the rectangular block as a ratio of the screen width
     */
    private static final float BLOCK_POSITION_RATIO = .08f;

    /**
     * Default initial bounce angle of the ball
     */
    private static final int DEFAULT_ANGLE = 30;

    /**
     * Default separator line width
     */
    static final float DIVIDING_LINE_SIZE = 1.f;

    /**
     * Ball movement speed
     */
    private static final int SPEED = 6;

    /**
     * Height and width of the rectangular block
     */
    private float blockHeight, blockWidth;

    /**
     * Ball radius
     */
    private static final float BALL_RADIUS = 8.f;

    private Paint blockPaint;

    private float blockLeft, racketLeft;

    private float cx, cy;

    private List<Point> pointList;

    private boolean isleft;

    private int angle;

    private int blockHorizontalNum;

    private int speed;


    public HitBlockView(Context context) {
        this(context, null);
    }

    public HitBlockView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HitBlockView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HitBlock);
        blockHorizontalNum = typedArray.getInt(R.styleable.HitBlock_block_horizontal_num, BLOCK_HORIZONTAL_NUM);
        speed = typedArray.getInt(R.styleable.HitBlock_ball_speed, SPEED);
        typedArray.recycle();
    }

    @Override
    protected void initConcreteView() {
        blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        blockPaint.setStyle(Paint.Style.FILL);

        blockHeight = screenHeight * BLOCK_HEIGHT_RATIO;
        blockWidth = screenWidth * BLOCK_WIDTH_RATIO;

        blockLeft = screenWidth * BLOCK_POSITION_RATIO;
        racketLeft = screenWidth * RACKET_POSITION_RATIO;

        controllerSize = (int) (blockHeight * 1.6f);
    }

    @Override
    protected void drawGame(Canvas canvas) {
        drawColorBlock(canvas);
        drawRacket(canvas);

        if (status == STATUS_GAME_PLAY || status == STATUS_GAME_FINISHED)
            makeBallPath(canvas);
    }

    @Override
     protected void resetConfigParams() {
        cx = racketLeft - 2 * BALL_RADIUS;
        cy = (int) (getHeight() * .5f);

        controllerPosition = DIVIDING_LINE_SIZE;

        angle = DEFAULT_ANGLE;

        isleft = true;

        if (pointList == null) {
            pointList = new ArrayList<>();
        } else {
            pointList.clear();
        }
    }

/**
 * Draw the racket
 * @param canvas The default canvas
 */
private void drawRacket(Canvas canvas) {
    mPaint.setColor(rModelColor);
    canvas.drawRect(racketLeft, controllerPosition, racketLeft + blockWidth, controllerPosition + controllerSize, mPaint);
}

/**
 * Draw and handle the trajectory of the ball
 * @param canvas The default canvas
 */
private void makeBallPath(Canvas canvas) {
    mPaint.setColor(mModelColor);

    if (cx <= blockLeft + blockHorizontalNum * blockWidth + (blockHorizontalNum - 1) * DIVIDING_LINE_SIZE + BALL_RADIUS) { // The ball enters the color block area
        if (checkTouchBlock(cx, cy)) { // Bounce back
            isleft = false;
        }
    }
    if (cx <= blockLeft + BALL_RADIUS ) { // The ball passes through the color block area
        isleft = false;
    }

    if (cx + BALL_RADIUS >= racketLeft && cx - BALL_RADIUS < racketLeft + blockWidth) { // The current X coordinate of the ball is within the X value range of the racket
        if (checkTouchRacket(cy)) { // The ball touches the racket
            if (pointList.size() == blockHorizontalNum * BLOCK_VERTICAL_NUM) { // All rectangular blocks are eliminated, the game is over
                status = STATUS_GAME_OVER;
                return;
            }
            isleft = true;
        }
    } else if (cx > canvas.getWidth()) { // The ball goes beyond the racket area
        status = STATUS_GAME_OVER;
    }

    if (cy <= BALL_RADIUS + DIVIDING_LINE_SIZE) { // The ball hits the top boundary
        angle = 180 - DEFAULT_ANGLE;
    } else if (cy >= getMeasuredHeight() - BALL_RADIUS - DIVIDING_LINE_SIZE) { // The ball hits the bottom boundary
        angle = 180 + DEFAULT_ANGLE;
    }

    if (isleft) {
        cx -= speed;
    } else {
        cx += speed;
    }
    cy -= (float) Math.tan(Math.toRadians(angle)) * speed;

    canvas.drawCircle(cx, cy, BALL_RADIUS, mPaint);

    invalidate();
}

/**
 * Check if the ball hits the racket
 * @param y The current Y coordinate of the ball
 * @return The ball is within the Y value range of the racket: true, otherwise: false
 */
private boolean checkTouchRacket(float y) {
    boolean flag = false;
    float diffVal = y - controllerPosition;
    if (diffVal >= 0 && diffVal <= controllerSize) { // The ball is within the Y value range of the racket
        flag = true;
    }
    return flag;
}

/**
 * Check if the ball hits a rectangular block
 * @param x The X coordinate of the ball
 * @param y The Y coordinate of the ball
 * @return Hit: true, otherwise: false
 */
private boolean checkTouchBlock(float x, float y) {
    int columnX = (int) ((x - blockLeft - BALL_RADIUS - speed ) / blockWidth);
    columnX = columnX == blockHorizontalNum ? columnX - 1 : columnX;
    int rowY = (int) (y / blockHeight);
    rowY = rowY == BLOCK_VERTICAL_NUM ? rowY - 1 : rowY;
    Point p = new Point();
    p.set(columnX, rowY);

    boolean flag = false;
    for (Point point : pointList) {
        if (point.equals(p.x, p.y)) {
            flag = true;
            break;
        }
    }

    if (!flag) {
        pointList.add(p);
    }
    return !flag;
}

/**
 * Draw the rectangular color block
 * @param canvas The default canvas
 */
private void drawColorBlock(Canvas canvas) {
    float left, top;
    int column, row, redCode, greenCode, blueCode;
    for (int i = 0; i < blockHorizontalNum * BLOCK_VERTICAL_NUM; i++) {
        row = i / blockHorizontalNum;
        column = i % blockHorizontalNum;


        boolean flag = false;
        for (Point point : pointList) {
            if (point.equals(column, row)) {
                flag = true;
                break;
            }
            
        }
        if (flag) {
            continue;

        }

            redCode = 255 - (255 - Color.red(lModelColor)) / (column + 1);
            greenCode = 255 - (255 - Color.green(lModelColor)) / (column + 1);
            blueCode = 255 - (255 - Color.blue(lModelColor)) / (column + 1);
            blockPaint.setColor(Color.rgb(redCode, greenCode, blueCode));

            left = blockLeft + column * (blockWidth + DIVIDING_LINE_SIZE);
            top = DIVIDING_LINE_SIZE + row * (blockHeight + DIVIDING_LINE_SIZE);
            canvas.drawRect(left, top, left + blockWidth, top + blockHeight, blockPaint);
        }
    }

}

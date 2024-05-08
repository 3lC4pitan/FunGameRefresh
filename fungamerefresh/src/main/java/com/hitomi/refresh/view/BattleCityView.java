package com.hitomi.refresh.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.SparseArray;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Created by Hitomis on 2016/3/09.
 * email:196425254@qq.com
 */
public class BattleCityView extends FunGameView {

    /**
     * Number of tracks
     */
    private static int TANK_ROW_NUM = 3;

    /**
     * Ratio of barrel size to tank size
     */
    private static final float TANK_BARREL_RATIO = 1 / 3.f;

    /**
     * Default spacing between bullets
     */
    private static final int DEFAULT_BULLET_NUM_SPACING = 360;

    /**
     * Default spacing between enemy tanks
     */
    private static final int DEFAULT_ENEMY_TANK_NUM_SPACING = 60;

    /**
     * Total number of missed enemy tanks and the increment of total number of tanks destroyed after upgrading
     */
    private static final int DEFAULT_TANK_MAGIC_TOTAL_NUM = 8;

    /**
     * Collection of enemy tank matrices on all tracks
     */
    private SparseArray<Queue<RectF>> eTankSparseArray;

    /**
     * Collection of coordinates of all bullets on the screen
     */
    private Queue<Point> mBulletList;

    /**
     * Coordinates of the bullet hitting the enemy tank
     */
    private Point usedBullet;

    /**
     * Used to randomly position a track index
     */
    private Random random;

    /**
     * Bullet radius
     */
    private float bulletRadius;

    /**
     * Spacing between enemy tanks and bullets
     */
    private int enemyTankSpace, bulletSpace;

    /**
     * Barrel size
     */
    private int barrelSize;

    /**
     * Enemy tank speed, bullet speed
     */
    private int enemySpeed = 2, bulletSpeed = 7;

    /**
     * Current offset between the previous and next existing enemy tank
     * Used to determine whether to send out a new enemy tank
     */
    private int offsetETankX;

    /**
     * Current offset between the previous and next bullet
     * Used to determine whether to fire a new bullet
     */
    private int offsetMBulletX;

    /**
     * Current number of missed tanks
     */
    private int overstepNum;

    /**
     * Number of tanks to be destroyed in the current difficulty level
     */
    private int levelNum;

    /**
     * Number of enemy tanks destroyed in the current difficulty level
     */
    private int wipeOutNum;

    /**
     * First mark value, used to add logic to add the first enemy tank
     */
    private boolean once = true;

    public BattleCityView(Context context) {
        this(context, null);
    }

    public BattleCityView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BattleCityView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void initConcreteView() {
        random = new Random();

        controllerSize = (int) (Math.floor((screenHeight * VIEW_HEIGHT_RATIO - (TANK_ROW_NUM + 1) * DIVIDING_LINE_SIZE) / TANK_ROW_NUM + .5f));
        barrelSize = (int) Math.floor(controllerSize * TANK_BARREL_RATIO + .5f);
        bulletRadius = (barrelSize - 2 * DIVIDING_LINE_SIZE) * .5f;

        resetConfigParams();
    }

    @Override
    protected void drawGame(Canvas canvas) {
        drawSelfTank(canvas);

        if (status == STATUS_GAME_PLAY || status == STATUS_GAME_FINISHED) {
            drawEnemyTank(canvas);
            makeBulletPath(canvas);
        }
    }

    @Override
    protected void resetConfigParams() {
        controllerPosition = DIVIDING_LINE_SIZE;

        status = FunGameView.STATUS_GAME_PREPAR;

        enemySpeed = 2;
        bulletSpeed = 7;

        levelNum = DEFAULT_TANK_MAGIC_TOTAL_NUM;
        wipeOutNum = 0;

        once = true;

        enemyTankSpace = controllerSize + barrelSize + DEFAULT_ENEMY_TANK_NUM_SPACING;
        bulletSpace = DEFAULT_BULLET_NUM_SPACING;

        eTankSparseArray = new SparseArray<>();
        for (int i = 0; i < TANK_ROW_NUM; i++) {
            Queue<RectF> rectFQueue = new LinkedList<>();
            eTankSparseArray.put(i, rectFQueue);
        }

        mBulletList = new LinkedList<>();
    }

    /**
     * Generate a Rect for drawing enemy tanks from the starting position on the left side based on the index track index
     *
     * @param index Track index
     * @return Enemy tank matrix
     */
    private RectF generateEnemyTank(int index) {
        float left = -(controllerSize + barrelSize);
        float top = index * (controllerSize + DIVIDING_LINE_SIZE) + DIVIDING_LINE_SIZE;
        return new RectF(left, top, left + barrelSize * 2.5f, top + controllerSize);
    }

    /**
     * Draw bullet path
     *
     * @param canvas Default canvas
     */
    private void makeBulletPath(Canvas canvas) {
        mPaint.setColor(mModelColor);
        offsetMBulletX += bulletSpeed;
        if (offsetMBulletX / bulletSpace == 1) {
            offsetMBulletX = 0;
        }

        if (offsetMBulletX == 0) {
            Point bulletPoint = new Point();
            bulletPoint.x = screenWidth - controllerSize - barrelSize;
            bulletPoint.y = (int) (controllerPosition + controllerSize * .5f);
            mBulletList.offer(bulletPoint);
        }

        boolean isOversetp = false;
        for (Point point : mBulletList) {
            if (checkWipeOutETank(point)) {
                usedBullet = point;
                continue;
            }
            if (point.x + bulletRadius <= 0) {
                isOversetp = true;
            }
            drawBullet(canvas, point);
        }

        if (isOversetp) {
            mBulletList.poll();
        }

        mBulletList.remove(usedBullet);
        usedBullet = null;
    }

    /**
     * Get the track index based on the Y coordinate
     *
     * @param y Y coordinate value
     * @return Track index
     */
    private int getTrackIndex(int y) {
        int index = y / (getMeasuredHeight() / TANK_ROW_NUM);
        index = index >= TANK_ROW_NUM ? TANK_ROW_NUM - 1 : index;
        index = index < 0 ? 0 : index;
        return index;
    }

    /**
     * Check if the bullet hits the enemy tank
     *
     * @param point Current bullet coordinate point
     * @return Hit: true, otherwise: false
     */
    private boolean checkWipeOutETank(Point point) {
        boolean beHit = false;
        int trackIndex = getTrackIndex(point.y);
        RectF rectF = eTankSparseArray.get(trackIndex).peek();
        if (rectF != null && rectF.contains(point.x, point.y)) {
            if (++wipeOutNum == levelNum) {
                upLevel();
            }
            eTankSparseArray.get(trackIndex).poll();
            beHit = true;
        }
        return beHit;
    }

    /**
     * Upgrade difficulty level
     */
    private void upLevel() {
        levelNum += DEFAULT_TANK_MAGIC_TOTAL_NUM;
        enemySpeed++;
        bulletSpeed += 2;
        wipeOutNum = 0;

        if (enemyTankSpace > 12)
            enemyTankSpace -= 12;

        if (bulletSpace > 30)
            bulletSpace -= 30;
    }

    /**
     * Draw bullet
     *
     * @param canvas Default canvas
     * @param point  Bullet center coordinate point
     */
    private void drawBullet(Canvas canvas, Point point) {
        point.x -= bulletSpeed;
        canvas.drawCircle(point.x, point.y, bulletRadius, mPaint);
    }

    /**
     * Check if our tank collides with enemy tanks
     *
     * @param index  Track index
     * @param selfX  X coordinate value of our tank
     * @param selfY  Top or bottom value of our tank matrix
     * @return true: Collision, otherwise: false
     */
    private boolean checkTankCrash(int index, float selfX, float selfY) {
        boolean isCrash = false;
        RectF rectF = eTankSparseArray.get(index).peek();
        if (rectF != null && rectF.contains(selfX, selfY)) {
            isCrash = true;
        }
        return isCrash;
    }

    /**
     * Draw our tank
     *
     * @param canvas Default canvas
     */
    private void drawSelfTank(Canvas canvas) {
        mPaint.setColor(rModelColor);
        boolean isAboveCrash = checkTankCrash(getTrackIndex((int) controllerPosition),
                screenWidth - controllerSize,
                controllerPosition);
        boolean isBelowCrash = checkTankCrash(getTrackIndex((int) (controllerPosition + controllerSize)),
                screenWidth - controllerSize,
                controllerPosition + controllerSize);

        if (isAboveCrash || isBelowCrash) {
            status = STATUS_GAME_OVER;
        }

        canvas.drawRect(screenWidth - controllerSize,
                controllerPosition,
                screenWidth,
                controllerPosition + controllerSize,
                mPaint);
        canvas.drawRect(screenWidth - controllerSize - barrelSize,
                controllerPosition + (controllerSize - barrelSize) * .5f,
                screenWidth - controllerSize,
                controllerPosition + (controllerSize - barrelSize) * .5f + barrelSize,
                mPaint);
    }

    /**
     * Draw enemy tanks on three tracks
     *
     * @param canvas Default canvas
     */
    private void drawEnemyTank(Canvas canvas) {
        mPaint.setColor(lModelColor);
        offsetETankX += enemySpeed;
        if (offsetETankX / enemyTankSpace == 1 || once) {
            offsetETankX = 0;
            once = false;
        }

        boolean isOverstep = false;
        int option = apperanceOption();
        for (int i = 0; i < TANK_ROW_NUM; i++) {
            Queue<RectF> rectFQueue = eTankSparseArray.get(i);

            if (offsetETankX == 0 && i == option) {
                rectFQueue.offer(generateEnemyTank(i));
            }

            for (RectF rectF : rectFQueue) {
                if (rectF.left >= screenWidth) {
                    isOverstep = true;
                    if (++overstepNum >= DEFAULT_TANK_MAGIC_TOTAL_NUM) {
                        status = STATUS_GAME_OVER;
                        break;
                    }
                    continue;
                }
                drawTank(canvas, rectF);
            }

            if (status == STATUS_GAME_OVER) break;
            if (isOverstep) {
                rectFQueue.poll();
                isOverstep = false;
            }
        }
        invalidate();
    }

    /**
     * Draw a single enemy tank
     *
     * @param canvas Default canvas
     * @param rectF  Tank matrix
     */
    private void drawTank(Canvas canvas, RectF rectF) {
        rectF.set(rectF.left + enemySpeed, rectF.top, rectF.right + enemySpeed, rectF.bottom);
        canvas.drawRect(rectF, mPaint);
        float barrelTop = rectF.top + (controllerSize - barrelSize) * .5f;
        canvas.drawRect(rectF.right, barrelTop, rectF.right + barrelSize, barrelTop + barrelSize, mPaint);

    }

    /**
     * Randomly position a track index
     *
     * @return Track index
     */
    private int apperanceOption() {
        return random.nextInt(TANK_ROW_NUM);
    }

}

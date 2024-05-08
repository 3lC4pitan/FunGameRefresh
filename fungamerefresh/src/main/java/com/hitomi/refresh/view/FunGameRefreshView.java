package com.hitomi.refresh.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.view.ViewCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.LinearLayout;

/**
 * Created by Hitomis on 2016/3/2.
 */
public class FunGameRefreshView extends LinearLayout implements View.OnTouchListener {
    /**
     * Pull-down state
     */
    public static final int STATUS_PULL_TO_REFRESH = 0;

    /**
     * Release to refresh state
     */
    public static final int STATUS_RELEASE_TO_REFRESH = 1;

    /**
     * Refreshing state
     */
    public static final int STATUS_REFRESHING = 2;

    /**
     * Release and hold to play game state
     */
    public static final int STATUS_AGAIN_DOWN = 3;

    /**
     * Refresh completed state
     */
    public static final int STATUS_REFRESH_FINISHED = 4;

    /**
     * Sticky ratio of pull-down drag
     */
    private static final float STICK_RATIO = .65f;

    /**
     * Callback interface for pull-down refresh
     */
    private FunGameRefreshListener mListener;

    /**
     * Header view for pull-down
     */
    private FunGameHeader header;

    /**
     * View to be pulled down for refresh
     */
    private View contentView;

    /**
     * Layout parameters of pull-down control
     */
    private MarginLayoutParams headerLayoutParams;

    /**
     * Height of pull-down control
     */
    private int hideHeaderHeight;

    /**
     * Current status
     */
    private int currentStatus = STATUS_REFRESH_FINISHED;

    /**
     * Screen vertical coordinate when finger is pressed down
     */
    private float preDownY;

    /**
     * Used to control initialization in onLayout, loaded only once
     */
    private boolean once;

    /**
     * Whether pulling down is currently possible
     */
    private boolean ableToPull;

    /**
     * Whether the refresh thread task is completed
     */
    private boolean isExecComplete;

    private int tempHeaderTopMargin;

    public FunGameRefreshView(Context context) {
        this(context, null);
    }

    public FunGameRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FunGameRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (getChildCount() > 1)
            throw new RuntimeException("FunGameRefreshView can only contain one View");
        setOrientation(VERTICAL);
        initView(context, attrs);
    }

    private void initView(Context context, AttributeSet attrs) {
        header = new FunGameHeader(context, attrs);
        addView(header, 0);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed && !once) {
            hideHeaderHeight = -header.getHeight();
            headerLayoutParams = (MarginLayoutParams) header.getLayoutParams();
            headerLayoutParams.topMargin = hideHeaderHeight;
            contentView = getChildAt(1);
            contentView.setOnTouchListener(this);
            once = true;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        checkAblePull(event);
        if (!ableToPull) return false;
        if (currentStatus == STATUS_AGAIN_DOWN) {
            return handleAgainDownAction(event);
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                preDownY = event.getRawY();
                if (currentStatus == STATUS_REFRESHING) { // Indicates that when released, it is in the refreshing state, and then pressed again
                    currentStatus = STATUS_AGAIN_DOWN;
                    setHeaderTopMarign(0);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float currY = event.getRawY();
                float distance = currY - preDownY;
                float offsetY = distance * STICK_RATIO;
                if (distance <= 0 && headerLayoutParams.topMargin <= hideHeaderHeight) {
                    return false;
                }

                if (headerLayoutParams.topMargin > 0) { // When the head is completely pulled out, the status is changed to release refresh
                    currentStatus = STATUS_RELEASE_TO_REFRESH;
                }

                if (headerLayoutParams.topMargin > 0) {
                    currentStatus = STATUS_RELEASE_TO_REFRESH;
                } else {
                    currentStatus = STATUS_PULL_TO_REFRESH;
                }

                // Offset the topMargin value of the pull-down head to achieve the pull-down effect
                setHeaderTopMarign((int) (offsetY + hideHeaderHeight));

                break;
            case MotionEvent.ACTION_UP:
                if (currentStatus == STATUS_PULL_TO_REFRESH) {
                    rollbackHeader(false);
                }
                if (currentStatus == STATUS_RELEASE_TO_REFRESH) {
                    rollBack2Header(true);
                }
                break;
        }
        if (currentStatus == STATUS_PULL_TO_REFRESH || currentStatus == STATUS_RELEASE_TO_REFRESH) {
            // Make contentView lose focus and unable to be clicked
            disableContentView();
            return true;
        }
        return false;
    }

    /**
     * Set topMargin parameter for header
     *
     * @param margin
     */
    private void setHeaderTopMarign(int margin) {
        headerLayoutParams.topMargin = margin;
        header.setLayoutParams(headerLayoutParams);
    }

    /**
     * Disable contentView, make it lose focus and not accept clicks
     */
    private void disableContentView() {
        contentView.setPressed(false);
        contentView.setFocusable(false);
        contentView.setFocusableInTouchMode(false);
    }

    /**
     * Handle the event of pressing the screen again to play the game
     *
     * @param event
     * @return
     */
    private boolean handleAgainDownAction(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentStatus = STATUS_AGAIN_DOWN;
                preDownY = event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                float currY = event.getRawY();
                float distance = currY - preDownY;
                float offsetY = distance * STICK_RATIO;
                header.moveRacket(offsetY);
                setHeaderTopMarign((int) (offsetY));
                break;
            case MotionEvent.ACTION_UP:
                currentStatus = STATUS_REFRESHING;
                if (isExecComplete) {
                    rollbackHeader(false);
                } else {
                    rollBack2Header(false);
                }
                break;
        }
        disableContentView();
        return true;
    }

    /**
     * The first execution in onTouch, so that the current scroll should be to the contentView, or should be pulled down.
     *
     * @param event
     */
    private void checkAblePull(MotionEvent event) {
        if (contentView != null) {
            if (!canContentViewScrollUp()) {
                if (!ableToPull) {
                    preDownY = event.getRawY();
                }
                ableToPull = true;
            } else { // Otherwise
                if (headerLayoutParams.topMargin != hideHeaderHeight) {
                    setHeaderTopMarign(hideHeaderHeight);
                }
                ableToPull = false;
            }
        } else {
            ableToPull = true;
        }
    }

    public boolean canContentViewScrollUp() {
        if (Build.VERSION.SDK_INT < 14) {
            if (contentView instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) contentView;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 || absListView.getChildAt(0)
                        .getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(contentView, -1) || contentView.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(contentView, -1);
        }
    }

    /**
     * Register a listener for pull-down refresh control.
     *
     * @param listener Implementation of the listener.
     */
    public void setOnRefreshListener(FunGameRefreshListener listener) {
        mListener = listener;
    }

    /**
     * After all refresh logic is completed, call this method to record it, otherwise it will always be in the refreshing state.
     */
    public void finishRefreshing() {
        header.postComplete();
        isExecComplete = true;
        if (currentStatus != STATUS_AGAIN_DOWN) {
            rollbackHeader(true);
        }
    }

    /**
     * Roll back to the height of the header refresh control and trigger the background refresh task
     */
    private void rollBack2Header(boolean isRefresh) {
        ValueAnimator rbToHeaderAnimator = ValueAnimator.ofInt(headerLayoutParams.topMargin, 0);
        long duration = (long) (headerLayoutParams.topMargin * 1.1f) >= 0 ? (long) (headerLayoutParams.topMargin * 1.1f) : 0;
        rbToHeaderAnimator.setDuration(duration);
        rbToHeaderAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rbToHeaderAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int marginValue = Integer.parseInt(animation.getAnimatedValue().toString());
                setHeaderTopMarign(marginValue);
            }
        });

        if (isRefresh)
            rbToHeaderAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    new AsyncTask<Void, Void, Void>() {

                        @Override
                        protected void onPreExecute() {
                            currentStatus = STATUS_REFRESHING;
                            header.postStart();
                        }

                        @Override
                        protected Void doInBackground(Void... params) {
                            if (mListener != null) {
                                final long minTimes = 1500;
                                long startTimes = System.currentTimeMillis();
                                mListener.onPullRefreshing();
                                long diffTimes = System.currentTimeMillis() - startTimes;
                                if (diffTimes < minTimes) {
                                    SystemClock.sleep(minTimes - diffTimes);
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            if (mListener != null)
                                mListener.onRefreshComplete();
                            finishRefreshing();
                        }
                    }.execute();
                }
            });
        header.back2StartPoint(duration);
        rbToHeaderAnimator.start();
    }


    /**
     * Roll back the pull-down refresh header control
     */
    private void rollbackHeader(boolean isDelay) {
        tempHeaderTopMargin = headerLayoutParams.topMargin;
        ValueAnimator rbAnimator = ValueAnimator.ofInt(0, header.getHeight() + tempHeaderTopMargin);
        rbAnimator.setDuration(300);
        rbAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        rbAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int marginValue = Integer.parseInt(animation.getAnimatedValue().toString());
                setHeaderTopMarign(-marginValue + tempHeaderTopMargin);
            }
        });
        rbAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (currentStatus == STATUS_PULL_TO_REFRESH || currentStatus == STATUS_REFRESH_FINISHED) {
                    currentStatus = STATUS_REFRESH_FINISHED;
                    return;
                }
                currentStatus = STATUS_REFRESH_FINISHED;
                isExecComplete = false;
                header.postEnd();
            }
        });
        if (isDelay)
            rbAnimator.setStartDelay(500);
        rbAnimator.start();
    }

    /**
     * Set loading start text
     *
     * @param loadingText
     */
    public void setLoadingText(String loadingText) {
        if (TextUtils.isEmpty(loadingText)) return;
        header.setHeaderLodingStr(loadingText);
    }

    /**
     * Set loading end text
     *
     * @param loadingFinishedText
     */
    public void setLoadingFinishedText(String loadingFinishedText) {
        if (TextUtils.isEmpty(loadingFinishedText)) return;
        header.setHeaderLoadingFinishedStr(loadingFinishedText);

    }

    /**
     * Set game over text
     *
     * @param gameOverText
     */
    public void setGameOverText(String gameOverText) {
        if (TextUtils.isEmpty(gameOverText)) return;
        header.setHeaderGameOverStr(gameOverText);
    }

    /**
     * Set text in top curtain
     *
     * @param topMaskText
     */
    public void setTopMaskText(String topMaskText) {
        if (TextUtils.isEmpty(topMaskText)) return;
        header.setTopMaskViewText(topMaskText);
    }

    /**
     * Set text in bottom curtain
     *
     * @param bottomMaskText
     */
    public void setBottomMaskText(String bottomMaskText) {
        if (TextUtils.isEmpty(bottomMaskText)) return;
        header.setBottomMaskViewText(bottomMaskText);
    }

    /**
     * Listener for pull-down refresh, this listener should be registered to get the refresh callback where pull-down refresh is used.
     */
    public interface FunGameRefreshListener {
        /**
         * Callback method when refreshing
         */
        void onPullRefreshing();

        void onRefreshComplete();
    }
}

package com.example.burge.meizhunotification.notification;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.IntEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ImageView;

import java.util.Date;
import java.util.Locale;

import com.example.burge.meizhunotification.R;

import java.text.SimpleDateFormat;

/**
 * Created by _SOLID
 * Date:2016/6/13
 * Time:11:25
 */
public class MeiZhuNotification implements View.OnTouchListener {

    private static final int DIRECTION_LEFT = -1;
    private static final int DIRECTION_NONE = 0;
    private static final int DIRECTION_RIGHT = 1;

    private static final int DISMISS_INTERVAL = 3000;

    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private View mContentView;
    private Context mContext;
    private int mScreenWidth = 0;
    private int mStatusBarHeight = 0;

    private boolean isShowing = false;
    private ValueAnimator restoreAnimator = null;
    private ValueAnimator dismissAnimator = null;
    private ImageView mIvIcon;
    private TextView mTvTitle;
    private TextView mTvContent;
    private TextView mTvTime;


    public MeiZhuNotification(Builder builder) {
        mContext = builder.getContext();

        mStatusBarHeight = getStatusBarHeight();
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.type = WindowManager.LayoutParams.TYPE_TOAST;// 系统提示window
        mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
        mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        //设置进入和退出动画
        mWindowParams.windowAnimations = R.style.NotificationAnim;
        mWindowParams.x = 0;
        mWindowParams.y = -mStatusBarHeight;

        setContentView(mContext, builder);
    }


    private static final int HIDE_WINDOW = 0;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_WINDOW:
                    dismiss();
                    break;
            }
            return false;
        }
    });

    /***
     * 设置内容视图
     *
     * @param context
     */
    private void setContentView(Context context, Builder builder) {
        mContentView = LayoutInflater.from(context).inflate(R.layout.layout_notification, null);
        View v_state_bar = mContentView.findViewById(R.id.v_state_bar);
        ViewGroup.LayoutParams layoutParameter = v_state_bar.getLayoutParams();
        layoutParameter.height = mStatusBarHeight;
        v_state_bar.setLayoutParams(layoutParameter);

        mIvIcon = (ImageView) mContentView.findViewById(R.id.iv_icon);
        mTvTitle = (TextView) mContentView.findViewById(R.id.tv_title);
        mTvContent = (TextView) mContentView.findViewById(R.id.tv_content);
        mTvTime = (TextView) mContentView.findViewById(R.id.tv_time);

        setIcon(builder.imgRes);
        setTitle(builder.title);
        setContent(builder.content);
        setTime(builder.time);

        mContentView.setOnTouchListener(this);
    }


    private int downX = 0;
    private int direction = DIRECTION_NONE;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (isAnimatorRunning()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = (int) event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                //处于滑动状态就取消自动消失
                mHandler.removeMessages(HIDE_WINDOW);
                int moveX = (int) event.getRawX() - downX;
                //判断滑动方向
                if (moveX > 0) {
                    direction = DIRECTION_RIGHT;
                } else {
                    direction = DIRECTION_LEFT;
                }

                updateWindowLocation(moveX, mWindowParams.y);

                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(mWindowParams.x) > mScreenWidth / 2) {
                    startDismissAnimator(direction);
                } else {
                    startRestoreAnimator();
                }
                break;
        }
        return true;
    }

    private void startRestoreAnimator() {
        restoreAnimator = new ValueAnimator().ofInt(mWindowParams.x, 0);
        restoreAnimator.setDuration(300);
        restoreAnimator.setEvaluator(new IntEvaluator());

        restoreAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                System.out.println("onAnimationUpdate:" + animation.getAnimatedValue());
                updateWindowLocation((Integer) animation.getAnimatedValue(), -mStatusBarHeight);
            }
        });
        restoreAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                restoreAnimator = null;
                autoDismiss();
            }
        });
        restoreAnimator.start();
    }

    private void startDismissAnimator(int direction) {
        if (direction == DIRECTION_LEFT)
            dismissAnimator = new ValueAnimator().ofInt(mWindowParams.x, -mScreenWidth);
        else {
            dismissAnimator = new ValueAnimator().ofInt(mWindowParams.x, mScreenWidth);
        }
        dismissAnimator.setDuration(300);
        dismissAnimator.setEvaluator(new IntEvaluator());

        dismissAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateWindowLocation((Integer) animation.getAnimatedValue(), -mStatusBarHeight);
            }
        });
        dismissAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                restoreAnimator = null;
                dismiss();
            }
        });
        dismissAnimator.start();
    }

    private boolean isAnimatorRunning() {
        return (restoreAnimator != null && restoreAnimator.isRunning()) || (dismissAnimator != null && dismissAnimator.isRunning());
    }

    public void updateWindowLocation(int x, int y) {
        if (isShowing) {
            mWindowParams.x = x;
            mWindowParams.y = y;
            mWindowManager.updateViewLayout(mContentView, mWindowParams);
        }

    }

    public void show() {
        if (!isShowing) {
            isShowing = true;
            mWindowManager.addView(mContentView, mWindowParams);
            autoDismiss();
        }
    }

    public void dismiss() {
        if (isShowing) {
            resetState();
            mWindowManager.removeView(mContentView);
        }
    }

    /**
     * 重置状态
     */
    private void resetState() {
        isShowing = false;
        mWindowParams.x = 0;
        mWindowParams.y = -mStatusBarHeight;
    }

    /**
     * 自动隐藏通知
     */
    private void autoDismiss() {
        mHandler.removeMessages(HIDE_WINDOW);
        mHandler.sendEmptyMessageDelayed(HIDE_WINDOW, DISMISS_INTERVAL);
    }

    /**
     * 获取状态栏的高度
     */
    public int getStatusBarHeight() {
        int height = 0;
        int resId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            height = mContext.getResources().getDimensionPixelSize(resId);
        }
        return height;
    }


    public void setIcon(int imgRes) {
        if (-1 != imgRes) {
            mIvIcon.setVisibility(View.VISIBLE);
            mIvIcon.setImageResource(imgRes);
        }
    }

    public void setTitle(String title) {
        if (!TextUtils.isEmpty(title)) {
            mTvTitle.setVisibility(View.VISIBLE);
            mTvTitle.setText(title);
        }
    }

    public void setContent(String content) {
        mTvContent.setText(content);
    }

    public void setTime(long time) {
        SimpleDateFormat formatDateTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        mTvTime.setText(formatDateTime.format(new Date(time)));
    }

    public static class Builder {

        private Context context;
        private int imgRes = -1;
        private String title;
        private String content = "none";
        private long time = System.currentTimeMillis();

        public Context getContext() {
            return context;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setImgRes(int imgRes) {
            this.imgRes = imgRes;
            return this;
        }


        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }


        public Builder setContent(String content) {
            this.content = content;
            return this;
        }


        public Builder setTime(long time) {
            this.time = time;
            return this;
        }


        public MeiZhuNotification build() {

            if (null == context)
                throw new IllegalArgumentException("the context is required.");

            return new MeiZhuNotification(this);
        }


    }

}

package me.ajax.bezierrotateview.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import me.ajax.bezierrotateview.utils.GeometryUtils;

import static me.ajax.bezierrotateview.utils.GeometryUtils.polarX;
import static me.ajax.bezierrotateview.utils.GeometryUtils.polarY;

/**
 * Created by aj on 2018/4/2
 */

public class BezierRotateView extends View {

    Paint circlePaint = new Paint();
    Paint pointPaint = new Paint();
    Paint linePaint = new Paint();

    float bigCircleRadius = dp2Dx(60);
    float smallCircleRadius = dp2Dx(10);
    int maxDistance = dp2Dx(100);

    int degree = 0;
    int animationValue = (int) (maxDistance * 7.81F);//初始化

    Path mPath = new Path();


    public BezierRotateView(Context context) {
        super(context);
        init();
    }

    public BezierRotateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BezierRotateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);//关闭硬件加速

        //画笔
        circlePaint.setColor(Color.WHITE);
        pointPaint.setColor(Color.RED);
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(dp2Dx(1));

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                //旋转动画
                ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
                animator.setDuration(5000);
                animator.setRepeatCount(Integer.MAX_VALUE - 1);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        degree++;
                    }
                });
                animator.start();

                //小球动画
                animator = ValueAnimator.ofInt(0, maxDistance * 8, maxDistance * 8, 0);
                animator.setDuration(10000);
                animator.setInterpolator(new LinearInterpolator());
                animator.setRepeatCount(Integer.MAX_VALUE - 1);
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        animationValue = (int) animation.getAnimatedValue();


                        float fraction = animationValue / (maxDistance * 8F * 2F);
                        bigCircleRadius = dp2Dx(60) - dp2Dx(35) * fraction;
                        invalidateView();
                    }
                });
                animator.start();
            }
        });
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int mWidth = getWidth();
        int mHeight = getHeight();

        canvas.save();
        canvas.translate(mWidth / 2, mHeight / 2);
        canvas.drawCircle(0, 0, bigCircleRadius, circlePaint);


        for (int i = 0; i < 8; i++) {

            double angle = i * 45 - 90 + degree;
            float p;
            if (animationValue / maxDistance > i) {
                p = maxDistance;
            } else if (animationValue / maxDistance < i) {
                p = 0;
            } else {
                p = animationValue % maxDistance;
            }

            canvas.drawCircle(polarX(p, angle), polarY(p, angle), smallCircleRadius, circlePaint);

            if (animationValue / maxDistance == i) {
                //画贝塞尔
                drawBezier(canvas, p, angle);
            }
        }

        canvas.restore();
    }

    private void drawBezier(Canvas canvas, float p, double angle) {

        if (p + smallCircleRadius < bigCircleRadius) return;

        float tempCircleRadius = smallCircleRadius;
        float percent = Math.abs(p + smallCircleRadius - bigCircleRadius)
                / Math.abs(maxDistance - bigCircleRadius + smallCircleRadius);

        mPath.reset();

        //切点1
        float tan1X = polarX(bigCircleRadius, angle - 30);
        float tan1Y = polarY(bigCircleRadius, angle - 30);

        //切点2
        float tan2X = polarX(bigCircleRadius, angle + 30);
        float tan2Y = polarY(bigCircleRadius, angle + 30);


        //角度变换
        double angleChange;
        if (percent < 0.5) {
            angleChange = 180 * percent;
        } else {
            angleChange = 180 * (0.5 - (percent - 0.5));

            //走到一半  p 会变小
            float half = bigCircleRadius + (maxDistance - bigCircleRadius) / 2;
            p = half - (p + smallCircleRadius - half);

            //走到一半  临时圆的半径会变小
            tempCircleRadius = tempCircleRadius * (1F - (percent - 0.5F));

            //大小有点不合适，灵活处理一下
            tempCircleRadius *= 0.6F;
        }
        //切点3
        float tan3X = polarX(p, angle) + polarX(tempCircleRadius, angle - angleChange);
        float tan3Y = polarY(p, angle) + polarY(tempCircleRadius, angle - angleChange);

        //切点4
        float tan4X = polarX(p, angle) + polarX(tempCircleRadius, angle + angleChange);
        float tan4Y = polarY(p, angle) + polarY(tempCircleRadius, angle + angleChange);

        //交点
        float intersectionX = GeometryUtils.intersectionX(tan1X, tan1Y, tan4X, tan4Y, tan2X, tan2Y, tan3X, tan3Y);
        float intersectionY = GeometryUtils.intersectionY(tan1X, tan1Y, tan4X, tan4Y, tan2X, tan2Y, tan3X, tan3Y);

        //绘制路径
        mPath.moveTo(tan1X, tan1Y);
        mPath.quadTo(intersectionX, intersectionY, tan3X, tan3Y);
        mPath.lineTo(tan4X, tan4Y);
        mPath.quadTo(intersectionX, intersectionY, tan2X, tan2Y);
        mPath.close();
        canvas.drawPath(mPath, circlePaint);

        //绘制临时圆
        canvas.drawCircle(polarX(p, angle), polarY(p, angle), tempCircleRadius, circlePaint);
    }


    int dp2Dx(int dp) {
        //return dp;
        return (int) (getResources().getDisplayMetrics().density * dp);
    }

    int dx2Dp(int dx) {
        return dx / (int) (getResources().getDisplayMetrics().density);
    }

    void l(Object o) {
        Log.e("######", o.toString());
    }


    private void invalidateView() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            //  当前线程是主UI线程，直接刷新。
            invalidate();
        } else {
            //  当前线程是非UI线程，post刷新。
            postInvalidate();
        }
    }
/*
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimAndRemoveCallbacks();
    }

    private void stopAnimAndRemoveCallbacks() {

        if (waterDropAnimator != null) waterDropAnimator.end();
        if (waveAnimator1 != null) waveAnimator1.end();
        if (waveAnimator2 != null) waveAnimator2.end();
        if (waveAnimator3 != null) waveAnimator3.end();

        Handler handler = this.getHandler();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
 */
}

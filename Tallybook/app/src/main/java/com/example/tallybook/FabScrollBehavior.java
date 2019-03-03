package com.example.tallybook;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;

//控制fab的behavior，其中show和hide设为static实时控制fab动画
public class FabScrollBehavior extends FloatingActionButton.Behavior {
    private static boolean isAnimatorEnable = true;//是否开启动画

    public FabScrollBehavior(Context context, AttributeSet attributeSet){super();}

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View directTargetChild, @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(coordinatorLayout,child,directTargetChild,target,axes,type);
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type);
        if(dyConsumed > 10 && child.getVisibility() == View.VISIBLE){
            hide(child);
        }else if(dyConsumed < -10 && child.getVisibility() != View.VISIBLE){
            show(child);
        }
    }

    @Override
    public boolean onNestedFling(@NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child, @NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if(velocityY > 0 && child.getVisibility() == View.VISIBLE)
            hide(child);
        else if(velocityY < 0 && child.getVisibility() == View.INVISIBLE)
            show(child);
        return true;
    }

    //显示动画

    static void show(final View view){
        //出现动画：从无到有
        view.animate().cancel();
        if (isAnimatorEnable) {
            view.setAlpha(0f);
            view.setScaleY(0f);
            view.setScaleX(0f);

            view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(100)
                    .setInterpolator(new LinearOutSlowInInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            view.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {

                        }
                    });
        }
    }

    //隐藏动画

    static void hide(final View view){
        //从有到无（本app只会在滑动时触发此动画）
        view.animate().cancel();
        if(isAnimatorEnable) {
            view.animate()
                    .scaleY(0f)
                    .scaleX(0f)
                    .alpha(0f)
                    .setDuration(100)
                    .setInterpolator(new FastOutLinearInInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        private boolean mCancelled;

                        @Override
                        public void onAnimationStart(Animator animation) {
                            view.setVisibility(View.VISIBLE);
                            mCancelled = false;
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {
                            mCancelled = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            view.setVisibility(View.INVISIBLE);
                        }
                    });
        }
    }

    static void setAnimatorEnable(boolean isEnable){
        isAnimatorEnable = isEnable;
    }
}

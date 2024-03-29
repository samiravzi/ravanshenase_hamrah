package com.hope.commonsense.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;

/**
 * Created by User on 5/22/2017.
 */
public class RightAlignHorizontalListView extends HorizontalScrollView {

    private boolean mGravityRight = false;
    private boolean mAutoScrolling = false;

    public RightAlignHorizontalListView(Context context) {
        super(context);
    }

    public RightAlignHorizontalListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RightAlignHorizontalListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public View getChildView() {
        return getChildAt(0);
    }

    private int getScrollRange() {
        int scrollRange = 0;
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            scrollRange = Math.max(0, child.getWidth() - (getWidth() - getPaddingLeft() - getPaddingRight()));
        }
        return scrollRange;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // HorizontalScrollView is broken for Gravity.RIGHT. So we're fixing it.
        mAutoScrolling = false;
        int childWidth = getChildView().getWidth();
        super.onLayout(changed, left, top, right, bottom);
        int delta = getChildView().getWidth() - childWidth;
        View childView = getChildView();
        FrameLayout.LayoutParams p = (LayoutParams) childView.getLayoutParams();
        int horizontalGravity = p.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        int verticalGravity = p.gravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (horizontalGravity == Gravity.RIGHT) {
            if (getScrollRange() > 0) {
                mGravityRight = true;
                p.gravity = Gravity.LEFT | verticalGravity;
                childView.setLayoutParams(p);
                super.onLayout(changed, left, top, right, bottom);
            }
        } else if (mGravityRight) {
            if (getScrollRange() == 0) {
                mGravityRight = false;
                p.gravity = Gravity.RIGHT | verticalGravity;
                childView.setLayoutParams(p);
                super.onLayout(changed, left, top, right, bottom);
            }
        }
        if (mGravityRight && delta > 0) {
            scrollBy(delta, 0);
            mAutoScrolling = true;
        }
    }

    @Override
    public void computeScroll() {
        if (mAutoScrolling) return;
        super.computeScroll();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (mAutoScrolling) return;
        super.scrollTo(x, y);
    }

}

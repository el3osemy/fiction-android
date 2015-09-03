package at.yawk.fiction.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import lombok.Setter;

/**
 * @author yawkat
 */
public class InterceptTouchFrameLayout extends FrameLayout {
    @Setter
    private OnTouchListener onTouchInterceptedListener;

    public InterceptTouchFrameLayout(Context context) {
        super(context);
    }

    public InterceptTouchFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptTouchFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return onTouchInterceptedListener != null &&
               onTouchInterceptedListener.onTouch(this, ev);
    }
}

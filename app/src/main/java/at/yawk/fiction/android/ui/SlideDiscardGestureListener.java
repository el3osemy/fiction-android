package at.yawk.fiction.android.ui;

import android.app.Activity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import at.yawk.fiction.android.R;
import lombok.RequiredArgsConstructor;

/**
 */
@RequiredArgsConstructor
class SlideDiscardGestureListener extends GestureDetector.SimpleOnGestureListener {
    // http://stackoverflow.com/a/25952007

    private final Activity activity;

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        float slope = (e1.getY() - e2.getY()) / (e1.getX() - e2.getX());
        float angle = (float) Math.atan(slope);
        float angleInDegree = (float) Math.toDegrees(angle);
        if (e1.getX() - e2.getX() > 20 && Math.abs(velocityX) > 20) {
            // left to right
            if ((angleInDegree < 45 && angleInDegree > -45)) {
                activity.overridePendingTransition(0, R.anim.slide_out_left); // todo: right
                activity.finish();
            }
        } else if (e2.getX() - e1.getX() > 20 && Math.abs(velocityX) > 20) {
            // right to left fling
            if ((angleInDegree < 45 && angleInDegree > -45)) {
                activity.overridePendingTransition(0, R.anim.slide_out_left);
                activity.finish();

            }
        }
        return true;
    }
}
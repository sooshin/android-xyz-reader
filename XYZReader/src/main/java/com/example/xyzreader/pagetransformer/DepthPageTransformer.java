package com.example.xyzreader.pagetransformer;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * This depth animation fades the page out, and scales it down linearly,
 * which is invoked whenever a visible/attached page is scrolled.
 *
 * Reference: @see "https://developer.android.com/training/animation/screen-slide#java"
 */
public class DepthPageTransformer implements ViewPager.PageTransformer {

    private static final float MIN_SCALE = 0.75f;
    private static final float ALPHA_ZERO = 0f;
    private static final float ALPHA_ONE = 1f;
    private static final float SCALE_X = 1f;
    private static final float SCALE_Y = 1f;

    public void transformPage(@NonNull View view, float position) {
        int pageWidth = view.getWidth();

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(ALPHA_ZERO);

        } else if (position <= 0) { // [-1,0]
            // Use the default slide transition when moving to the left page
            view.setAlpha(ALPHA_ONE);
            view.setTranslationX(ALPHA_ZERO);
            view.setScaleX(SCALE_X);
            view.setScaleY(SCALE_Y);

        } else if (position <= 1) { // (0,1]
            // Fade the page out.
            view.setAlpha(1 - position);

            // Counteract the default slide transition
            view.setTranslationX(pageWidth * -position);

            // Scale the page down (between MIN_SCALE and 1)
            float scaleFactor = MIN_SCALE
                    + (1 - MIN_SCALE) * (1 - Math.abs(position));
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);

        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(ALPHA_ZERO);
        }
    }
}

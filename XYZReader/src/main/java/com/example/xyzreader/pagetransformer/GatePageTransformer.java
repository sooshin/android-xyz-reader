package com.example.xyzreader.pagetransformer;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

import static com.example.xyzreader.pagetransformer.DepthPageTransformer.ALPHA_ONE;
import static com.example.xyzreader.pagetransformer.DepthPageTransformer.ALPHA_ZERO;

/**
 * A GatePageTransformer provides gate animation needed for transforming ViewPager scrolling.
 *
 * Reference: @see "https://github.com/dipanshukr/Viewpager-Transformation/wiki/Gate-Transformation"
 */
public class GatePageTransformer implements ViewPager.PageTransformer {

    static final int ROTATION_NINETY = 90;
    static final int ROTATION_NINETY_N = -90;
    static final int PIVOT_X = 0;

    @Override
    public void transformPage(@NonNull View page, float position) {
        page.setTranslationX(-position*page.getWidth());

        if (position < -1){    // [-Infinity,-1)
            // This page is way off-screen to the left.
            page.setAlpha(ALPHA_ZERO);

        } else if (position <= 0){    // [-1,0]
            page.setAlpha(ALPHA_ONE);
            page.setPivotX(PIVOT_X);
            page.setRotationY(ROTATION_NINETY*Math.abs(position));

        } else if (position <= 1){    // (0,1]
            page.setAlpha(ALPHA_ONE);
            page.setPivotX(page.getWidth());
            page.setRotationY(ROTATION_NINETY_N*Math.abs(position));

        } else {    // (1,+Infinity]
            // This page is way off-screen to the right.
            page.setAlpha(ALPHA_ZERO);
        }
    }
}

package com.example.xyzreader.pagetransformer;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * A CubeOutPageTransformer provides cube animation needed for transforming ViewPager scrolling.
 *
 * reference: @see "https://github.com/dipanshukr/Viewpager-Transformation/wiki/Cube-Out-Transformation"
 */
public class CubeOutPageTransformer implements ViewPager.PageTransformer{

    private static final float ALPHA_ZERO = 0f;
    private static final float ALPHA_ONE = 1f;

    @Override
    public void transformPage(@NonNull View view, float position) {
        if (position < -1){    // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(ALPHA_ZERO);

        } else if (position <= 0) {    // [-1,0]
            view.setAlpha(ALPHA_ONE);
            view.setPivotX(view.getWidth());
            view.setRotationY(-90 * Math.abs(position));

        } else if (position <= 1){    // (0,1]
            view.setAlpha(ALPHA_ONE);
            view.setPivotX(0);
            view.setRotationY(90 * Math.abs(position));

        } else {    // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(ALPHA_ZERO);

        }
    }
}

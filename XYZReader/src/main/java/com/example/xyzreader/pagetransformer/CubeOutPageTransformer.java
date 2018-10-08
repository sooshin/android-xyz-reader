package com.example.xyzreader.pagetransformer;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

import static com.example.xyzreader.pagetransformer.DepthPageTransformer.ALPHA_ONE;
import static com.example.xyzreader.pagetransformer.DepthPageTransformer.ALPHA_ZERO;
import static com.example.xyzreader.pagetransformer.ZoomOutPageTransformer.PIVOT_X;
import static com.example.xyzreader.pagetransformer.ZoomOutPageTransformer.ROTATION_NINETY;
import static com.example.xyzreader.pagetransformer.ZoomOutPageTransformer.ROTATION_NINETY_N;

/**
 * A CubeOutPageTransformer provides cube out animation needed for transforming ViewPager scrolling.
 *
 * Reference: @see "https://github.com/dipanshukr/Viewpager-Transformation/wiki/Cube-Out-Transformation"
 */
public class CubeOutPageTransformer implements ViewPager.PageTransformer {

    @Override
    public void transformPage(@NonNull View view, float position) {
        if (position < -1){    // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(ALPHA_ZERO);

        }
        else if (position <= 0) {    // [-1,0]
            view.setAlpha(ALPHA_ONE);
            view.setPivotX(view.getWidth());
            view.setRotationY(ROTATION_NINETY_N * Math.abs(position));

        }
        else if (position <= 1){    // (0,1]
            view.setAlpha(ALPHA_ONE);
            view.setPivotX(PIVOT_X);
            view.setRotationY(ROTATION_NINETY * Math.abs(position));

        }
        else {    // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(ALPHA_ZERO);

        }
    }
}

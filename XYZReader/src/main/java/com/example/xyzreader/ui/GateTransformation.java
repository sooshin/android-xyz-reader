package com.example.xyzreader.ui;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * A GateTransformer is invoked whenever a visible/attached page is scrolled.
 *
 * Reference: @see "https://github.com/dipanshukr/Viewpager-Transformation/wiki/Gate-Transformation"
 */
public class GateTransformation implements ViewPager.PageTransformer {

    @Override
    public void transformPage(@NonNull View page, float position) {
        page.setTranslationX(-position*page.getWidth());

        if (position<-1){    // [-Infinity,-1)
            // This page is way off-screen to the left.
            page.setAlpha(0);

        } else if (position<=0){    // [-1,0]
            page.setAlpha(1);
            page.setPivotX(0);
            page.setRotationY(90*Math.abs(position));

        } else if (position <=1){    // (0,1]
            page.setAlpha(1);
            page.setPivotX(page.getWidth());
            page.setRotationY(-90*Math.abs(position));

        } else {    // (1,+Infinity]
            // This page is way off-screen to the right.
            page.setAlpha(0);

        }
    }
}

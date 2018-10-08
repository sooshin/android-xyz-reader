package com.example.xyzreader.pagetransformer;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * A PopPageTransformer provides pop animation needed for transforming ViewPager scrolling.
 *
 * Reference: @see "https://github.com/dipanshukr/Viewpager-Transformation/wiki/Pop-Transformation"
 */
public class PopPageTransformer implements ViewPager.PageTransformer {
    @Override
    public void transformPage(@NonNull View view, float position) {

        view.setTranslationX(-position * view.getWidth());

        if (Math.abs(position) < 0.5) {
            view.setVisibility(View.VISIBLE);
            view.setScaleX(1 - Math.abs(position));
            view.setScaleY(1 - Math.abs(position));
        } else if (Math.abs(position) > 0.5) {
            view.setVisibility(View.GONE);
        }

    }
}

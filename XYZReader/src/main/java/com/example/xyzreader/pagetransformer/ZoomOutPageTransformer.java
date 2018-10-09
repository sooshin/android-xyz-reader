/*
 * Copyright 2018 Soojeong Shin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.xyzreader.pagetransformer;

import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * A ZoomOutPageTransformer provides zoom out animation needed for transforming ViewPager scrolling.
 *
 * reference: @see "https://developer.android.com/training/animation/screen-slide#java"
 */
public class ZoomOutPageTransformer implements ViewPager.PageTransformer{

    private static final float MIN_SCALE = 0.85f;
    private static final float MIN_ALPHA = 0.5f;
    private static final float ALPHA_ZERO = 0f;
    private static final int NUMBER_TWO = 2;
    static final int ROTATION_NINETY = 90;
    static final int ROTATION_NINETY_N = -90;
    static final int PIVOT_X = 0;


    public void transformPage(@NonNull View view, float position) {
        int pageWidth = view.getWidth();
        int pageHeight = view.getHeight();

        if (position < -1) { // [-Infinity,-1)
            // This page is way off-screen to the left.
            view.setAlpha(ALPHA_ZERO);

        } else if (position <= 1) { // [-1,1]
            // Modify the default slide transition to shrink the page as well
            float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
            float vertMargin = pageHeight * (1 - scaleFactor) / NUMBER_TWO;
            float horzMargin = pageWidth * (1 - scaleFactor) / NUMBER_TWO;
            if (position < 0) {
                view.setTranslationX(horzMargin - vertMargin / NUMBER_TWO);
            } else {
                view.setTranslationX(-horzMargin + vertMargin / NUMBER_TWO);
            }

            // Scale the page down (between MIN_SCALE and 1)
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);

            // Fade the page relative to its size.
            view.setAlpha(MIN_ALPHA +
                    (scaleFactor - MIN_SCALE) /
                            (1 - MIN_SCALE) * (1 - MIN_ALPHA));

        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(ALPHA_ZERO);
        }
    }
}

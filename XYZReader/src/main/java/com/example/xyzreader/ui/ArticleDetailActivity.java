package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.pagetransformer.CubeOutPageTransformer;
import com.example.xyzreader.pagetransformer.DepthPageTransformer;

import java.util.List;
import java.util.Map;

import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_CURRENT_POSITION;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_PAGE_TRANSFORMATION;
import static com.example.xyzreader.ui.ArticleListActivity.EXTRA_STARTING_POSITION;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /** Constant string for saving the current state of this Activity */
    private static final String STATE_CURRENT_PAGE_POSITION = "state_current_page_position";
    /** Constant value used for the page transformation */
    public static final String GATE = "gate";
    public static final String CUBE = "cube";
    public static final String DEPTH = "depth";

    private Cursor mCursor;
    private long mStartId;

    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private View mUpButtonContainer;
    private View mUpButton;

    /** The current position in the ViewPager */
    private int mCurrentPosition;
    /** The position of a selected item in the ArticleListActivity */
    private int mStartingPosition;
    private boolean mIsReturning;
    /** Member variable for ArticleDetailFragment */
    private ArticleDetailFragment mCurrentDetailFragment;
    /** Member variable for PageTransformer */
    private ViewPager.PageTransformer mPageTransformer;
    /** A string for the page transformation currently set in Preferences */
    private String mPageTransformerStr;

    /**
     * Monitor the Shared element transitions to match the transition name when the user changes
     * the Fragment in the ViewPager.
     *
     * References: @see "https://github.com/alexjlockwood/adp-activity-transitions"
     * @see "https://discussions.udacity.com/t/trouble-implementing-shared-element-transition/674912/19"
     */
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mIsReturning) {
                // Get the shared element that should be transitioned back to the previous Activity
                ImageView sharedElement = mCurrentDetailFragment.getPhotoView();
                if (sharedElement == null) {
                    // If shared element is null, then it has been scrolled off screen and
                    // no longer visible. In this case we cancel the shared element transition by
                    // removing the shared element from the shared elements map.
                    names.clear();
                    sharedElements.clear();
                } else if (mStartingPosition != mCurrentPosition) {
                    // If the user has swiped to a different ViewPager page, then we need to
                    // remove the old shared element and replace it with the new shared element
                    // that should be transitioned instead.
                    names.clear();
                    names.add(sharedElement.getTransitionName());
                    sharedElements.clear();
                    sharedElements.put(sharedElement.getTransitionName(), sharedElement);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            // Postpone the shared element enter transition.
            // Although the transition is between activities, the shared element is included
            // in a Fragment, which is loaded by a ViewPager. The problem is that the animations happen
            // very early in the Activity Lifecycle. So, we need to postpone the transition until the
            // shared element has been properly loaded.
            // Reference: @see "https://discussions.udacity.com/t/trouble-implementing-shared-element-transition/674912/8"
            ActivityCompat.postponeEnterTransition(this);

            // SharedElementCallback will be called to handle shared elements on the launched Activity
            setEnterSharedElementCallback(mCallback);
        }
        setContentView(R.layout.activity_article_detail);

        // Get the position of the item clicked from the ArticleListActivity via Intent, which is
        // used to move the Cursor position to the clicked position in onLoadFinished().
        mStartingPosition = getIntent().getIntExtra(EXTRA_STARTING_POSITION, 0);
        if (savedInstanceState == null) {
            mCurrentPosition = mStartingPosition;
        } else {
            // Load the saved state
            mCurrentPosition = savedInstanceState.getInt(STATE_CURRENT_PAGE_POSITION);
        }

        // Get the PageTransformer string via Intent
        mPageTransformerStr = getIntent().getStringExtra(EXTRA_PAGE_TRANSFORMATION);

        getSupportLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setPageMargin((int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
        mPager.setPageMarginDrawable(new ColorDrawable(0x22000000));
        // Set the the currently selected page
        mPager.setCurrentItem(mCurrentPosition);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                mUpButton.animate()
                        .alpha((state == ViewPager.SCROLL_STATE_IDLE) ? 1f : 0f)
                        .setDuration(300);
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                }
                mSelectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                updateUpButtonPosition();

                // Update the current position if the position has changed.
                mCurrentPosition = position;
            }
        });

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reverses the Activity Scene entry Transition and triggers the calling Activity
                // to reverse its exit Transition.
                // References: @see "https://stackoverflow.com/questions/37713793/shared-element-transition-when-using-actionbar-back-button"
                // @see "https://discussions.udacity.com/t/transition-work-when-exiting-but-not-entering/207227/4"
                supportFinishAfterTransition();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                mSelectedItemId = mStartId;
            }
        }
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Optimize the code
        // Reference: @see "https://discussions.udacity.com/t/how-to-optimise-onloadfinished-in-detailactivity/213247/2"
        // Move the Cursor position to the clicked position
        mPager.setCurrentItem(mCurrentPosition, false);
        mCursor.moveToPosition(mCurrentPosition);

        // Apply Page Transformer to the page views using animation properties
        // References: @see "https://github.com/dipanshukr/Viewpager-Transformation"
        // @see "https://developer.android.com/training/animation/screen-slide#java"
        mPager.setPageTransformer(true, getPageTransformer());
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 0));
    }

    /**
     * Save the current state of this Activity.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store the current position to our bundle
        outState.putInt(STATE_CURRENT_PAGE_POSITION, mCurrentPosition);
    }

    /**
     * Set result when you are going to leave the ArticleDetailActivity.
     */
    @Override
    public void finishAfterTransition() {
        mIsReturning = true;
        Intent data = new Intent();
        data.putExtra(EXTRA_STARTING_POSITION, mStartingPosition);
        data.putExtra(EXTRA_CURRENT_POSITION, mCurrentPosition);
        setResult(RESULT_OK, data);
        super.finishAfterTransition();
    }

    /**
     * Returns PageTransformer which provides animation needed for transforming ViewPager scrolling.
     */
    public ViewPager.PageTransformer getPageTransformer() {
        if (!TextUtils.isEmpty(mPageTransformerStr)) {
            switch (mPageTransformerStr) {
                case GATE:
                    mPageTransformer = new GateTransformation();
                    break;
                case CUBE:
                    mPageTransformer = new CubeOutPageTransformer();
                    break;
                case DEPTH:
                    mPageTransformer = new DepthPageTransformer();
                    break;
                default:
                    mPageTransformer = new GateTransformation();
            }
        }
        return mPageTransformer;
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentDetailFragment = (ArticleDetailFragment) object;
            if (mCurrentDetailFragment != null) {
                mSelectedItemUpButtonFloor = mCurrentDetailFragment.getUpButtonFloor();
                updateUpButtonPosition();
            }
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID), position, mStartingPosition);
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}

package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static com.example.xyzreader.ui.ArticleDetailActivity.DEPTH;
import static com.example.xyzreader.ui.ArticleDetailActivity.EXTRA_LARGE;
import static com.example.xyzreader.ui.ArticleDetailActivity.GATE;
import static com.example.xyzreader.ui.ArticleDetailActivity.LARGE;
import static com.example.xyzreader.ui.ArticleDetailActivity.MEDIUM;
import static com.example.xyzreader.ui.ArticleDetailActivity.SMALL;
import static com.example.xyzreader.ui.ArticleDetailActivity.ZOOM;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = ArticleListActivity.class.toString();
    /** Key for Intent Extras */
    public static final String EXTRA_STARTING_POSITION = "extra_starting_position";
    public static final String EXTRA_CURRENT_POSITION = "extra_current_position";
    public static final String EXTRA_PAGE_TRANSFORMATION = "extra_page_transformation";
    public static final String EXTRA_TEXT_SIZE = "extra_text_size";

    /** Member variable for the PageTransformer string */
    private String mPageTransformerStr;
    /** Member variable for text size - small, medium, large, extra_large */
    private String mTextSizeStr;

    /** Bundle for result data from ArticleDetailActivity */
    private Bundle mReenterState;

    private Toolbar mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private CoordinatorLayout mCoordinatorLayout;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    public static final int XYZ_LOADER_ID = 0;

    /**
     * Monitor the Shared element transitions.
     *
     * References: @see "https://discussions.udacity.com/t/trouble-implementing-shared-element-transition/674912/19"
     * @see "https://android.jlelse.eu/dynamic-shared-element-transition-23428f62a2af"
     * @see "https://github.com/alexjlockwood/adp-activity-transitions"
     */
    private final SharedElementCallback mCallback = new SharedElementCallback() {
        // Lets the SharedElementCallback adjust the mapping of shared element names to Views.
        // Check if the position has changed. If so, remove the references to the old shared element
        // and add the new one.
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
            if (mReenterState != null) {
                // Get the starting position and the current position
                int startingPosition = mReenterState.getInt(EXTRA_STARTING_POSITION);
                int currentPosition = mReenterState.getInt(EXTRA_CURRENT_POSITION);
                Log.v(TAG, "starting: " + startingPosition);
                Log.v(TAG, "current: " + currentPosition);
                if (startingPosition != currentPosition) {
                    // If startingPosition is not equal to currentPosition, the user must have swiped to a
                    // different page in the ArticleDetailActivity. We must update the shared element
                    // so that the correct one falls into place.
                    String newTransitionName = getString(R.string.transition_photo) + currentPosition;
                    View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
                    if (newSharedElement != null) {
                        names.clear();
                        names.add(newTransitionName);
                        sharedElements.clear();
                        sharedElements.put(newTransitionName, newSharedElement);
                    }
                }

                mReenterState = null;
            } else {
                // If mTmpReenterState is null, then the activity is exiting.
                View navigationBar = findViewById(android.R.id.navigationBarBackground);
                View statusBar = findViewById(android.R.id.statusBarBackground);
                if (navigationBar != null) {
                    names.add(navigationBar.getTransitionName());
                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
                }
                if (statusBar != null) {
                    names.add(statusBar.getTransitionName());
                    sharedElements.put(statusBar.getTransitionName(), statusBar);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // SharedElementCallback will be called to handle shared elements on the launching Activity
            setExitSharedElementCallback(mCallback);
        }

        mCoordinatorLayout = findViewById(R.id.coordinator);
        // Show a snackbar message to indicate the network status
        showSnackbar(isConnected());

        mToolbar = findViewById(R.id.toolbar);
        // Set action bar to get the menu items to display in the toolbar
        // Reference: @see "https://stackoverflow.com/questions/13267030/oncreateoptionsmenu-is-never-called"
        setSupportActionBar(mToolbar);

        // Set the color scheme of the SwipeRefreshLayout and setup OnRefreshListener
        setSwipeRefreshLayout();

        mRecyclerView = findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(XYZ_LOADER_ID, null, this);

        if (savedInstanceState == null) {
            refresh();
        }

        // Get a string for the page transformation currently set in Preferences
        mPageTransformerStr = getPreferredPageTransformationStr();
        // Get a string for the text size currently set in Preferences
        mTextSizeStr = getPreferredTextSizeStr();
        // Register ArticleDetailActivity as an OnPreferenceChangedListener to receive a callback when a
        // SharedPreference has changed. Please note that we must unregister MainActivity as an
        // OnSharedPreferenceChanged listener in onDestroy to avoid any memory leaks.
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Returns a string for the page transformation currently set in Preferences.
     */
    private String getPreferredPageTransformationStr() {
        // Get all of the values from shared preferences to set it up
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // String for the key
        String keyForPageAnimation = getString(R.string.pref_page_animation_key);
        // String for the default value
        String defaultPageAnimation = getString(R.string.pref_page_animation_default);
        return prefs.getString(keyForPageAnimation, defaultPageAnimation);
    }

    /**
     * Returns a string for the text size currently set in Preferences.
     */
    private String getPreferredTextSizeStr() {
        // Get all of the values from shared preferences to set it up
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // String for the key
        String keyForTextSize = getString(R.string.pref_text_size_key);
        // String for the default value
        String defaultTextSize = getString(R.string.pref_text_size_default);
        return prefs.getString(keyForTextSize, defaultTextSize);
    }

    /**
     * Set the SwipeRefreshLayout triggered by a swipe gesture.
     */
    private void setSwipeRefreshLayout() {
        mSwipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.color_swipe_deep_purple),
                getResources().getColor(R.color.color_swipe_red));
        // The swipe gesture triggers a refresh
        // Reference: @see "https://discussions.udacity.com/t/swiperefreshlayout-indicator-remains-visible-after-repeated-swipes/161424"
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
                // Rerun the layout animation for RecyclerView
                runLayoutAnimation(mRecyclerView);
                // Hide refresh progress to signal refresh has finished
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    /**
     * Rerun the layout animation for RecyclerView.
     *
     * Reference: @see "https://proandroiddev.com/enter-animation-using-recyclerview-and-layoutanimation-part-1-list-75a874a5d213"
     */
    private void runLayoutAnimation(RecyclerView recyclerView) {
        Context context = recyclerView.getContext();
        LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_from_bottom);
        recyclerView.setLayoutAnimation(controller);
        recyclerView.getAdapter().notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    /**
     * Called when an ArticleDetailActivity you launched with an activity transition exposes
     * this ArticleListActivity through a returning activity transition.
     *
     * @param resultCode The integer result code returned by the child activity through its
     *                   setResult() which is set when leaving the ArticleDetailActivity
     * @param data An Intent, which can return result data to the caller
     */
    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        mReenterState = new Bundle(data.getExtras());
        int startingPosition = mReenterState.getInt(EXTRA_STARTING_POSITION);
        int currentPosition = mReenterState.getInt(EXTRA_CURRENT_POSITION);
        Log.v(TAG, "startingPosition: " + startingPosition);
        Log.v(TAG, "currentPosition: " + currentPosition);
        if (startingPosition != currentPosition) {
            // Scroll to the current position
            mRecyclerView.scrollToPosition(currentPosition);
        }
        // Postpone the shared element return transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityCompat.postponeEnterTransition(this);
        }
        // Start the postponed transition
        scheduleStartPostponedTransition(mRecyclerView);
    }

    /**
     * Start the postponed transition in an OnPreDrawListener, which will be called after
     * the RecyclerView has been properly loaded.
     */
    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                // Request layout in order to get a smooth transition
                mRecyclerView.requestLayout();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ActivityCompat.startPostponedEnterTransition(ArticleListActivity.this);
                }
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister ArticleListActivity as an OnPreferenceChangedListener to avoid any memory leaks
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.refresh:
                refresh();
                // Rerun the layout animation for RecyclerView
                runLayoutAnimation(mRecyclerView);
                return true;
            case R.id.action_settings:
                // Launch Settings activity
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Called when a shared preference is changed.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.pref_page_animation_key))) {
            String pageAnimation = sharedPreferences
                    .getString(key, getString(R.string.pref_page_animation_default));
            switch (pageAnimation) {
                case GATE:
                    mPageTransformerStr = getString(R.string.pref_page_animation_gate);
                    break;
                case ZOOM:
                    mPageTransformerStr = getString(R.string.pref_page_animation_zoom);
                    break;
                case DEPTH:
                    mPageTransformerStr = getString(R.string.pref_page_animation_depth);
                    break;
                default:
                    mPageTransformerStr = getString(R.string.pref_page_animation_gate);
            }

        } else if (key.equals(getString(R.string.pref_text_size_key))) {
            String textSize = sharedPreferences
                    .getString(key, getString(R.string.pref_text_size_default));
            switch (textSize) {
                case SMALL:
                    mTextSizeStr = getString(R.string.pref_text_size_small);
                    break;
                case MEDIUM:
                    mTextSizeStr = getString(R.string.pref_text_size_medium);
                    break;
                case LARGE:
                    mTextSizeStr = getString(R.string.pref_text_size_large);
                    break;
                case EXTRA_LARGE:
                    mTextSizeStr = getString(R.string.pref_text_size_extra_large);
                    break;
                default:
                    mTextSizeStr = getString(R.string.pref_text_size_medium);
            }
        }
    }

    /**
     * Returns true if there is the network connectivity.
     */
    private boolean isConnected() {
        // Get a reference to the ConnectivityManager to check state of network connectivity
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get details on the currently active default data network
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    /**
     * Show a snackbar message to indicate the network status.
     * @param isConnected True if connected to the network
     */
    private void showSnackbar(boolean isConnected) {
        String snackMessage;
        if (isConnected) {
            snackMessage = getString(R.string.snackbar_online);
        } else {
            snackMessage = getString(R.string.snackbar_offline);
        }
        Snackbar.make(mCoordinatorLayout, snackMessage, Snackbar.LENGTH_LONG).show();
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));
                    // Pass the starting position where the user clicks in the ArticleListActivity
                    intent.putExtra(EXTRA_STARTING_POSITION, vh.getAdapterPosition());
                    // Pass the page transformer string
                    intent.putExtra(EXTRA_PAGE_TRANSFORMATION, mPageTransformerStr);
                    // Pass the text size
                    intent.putExtra(EXTRA_TEXT_SIZE, mTextSizeStr);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // Apply the shared element transition to the thumbnail image
                        // Reference: @see "https://discussions.udacity.com/t/trouble-implementing-shared-element-transition/674912/2"
                        String transitionName = vh.thumbnailView.getTransitionName();
                        Log.v(TAG, "transition Name: " + transitionName);
                        Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                ArticleListActivity.this,
                                vh.thumbnailView,
                                transitionName
                        ).toBundle();
                        startActivity(intent, bundle);
                    } else {
                        startActivity(intent);
                    }
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            // Set text size based on the Value in SharedPreferences
            setTextSize(holder);

            // Use Picasso library to load the images
            // since the images load better when using Picasso instead of the ImageLoaderHelper
            // Reference: @see "https://discussions.udacity.com/t/need-help-implementing-transition-animation/219077/9"
            Picasso.with(ArticleListActivity.this)
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .error(R.drawable.photo_error)
                    .into(holder.thumbnailView);
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            // Set unique transition name for each image
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.thumbnailView.setTransitionName(getString(R.string.transition_photo) + position);
            }
            holder.thumbnailView.setTag(getString(R.string.transition_photo) + position);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }

    /**
     * Set text size based on the Value in SharedPreferences.
     */
    private void setTextSize(ViewHolder holder) {
        if (mTextSizeStr.equals(getString(R.string.pref_text_size_small))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp14));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp12));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_medium))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp16));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp14));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_large))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp18));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp16));
        } else if (mTextSizeStr.equals(getString(R.string.pref_text_size_extra_large))) {
            holder.titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp20));
            holder.subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, holder.itemView.getContext()
                    .getResources().getDimension(R.dimen.sp18));
        }
    }
}

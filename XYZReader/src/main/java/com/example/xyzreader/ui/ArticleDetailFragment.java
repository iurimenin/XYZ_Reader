package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AppBarLayout.OnOffsetChangedListener {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final String APPBAR_EXPANDED = "APPBAR_EXPANDED";
    private static final String APPBAR_COLLAPSED = "APPBAR_COLLAPSED";
    private static final String APPBAR_IDLE = "APPBAR_IDLE";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private String mAppBarCurrentState = APPBAR_IDLE;

    @BindView(R.id.photo) ImageView mPhotoView;
    @BindView(R.id.article_body) TextView mBodyView;
    @BindView(R.id.article_title) TextView mTitleView;
    @BindView(R.id.article_byline) TextView mBylineView;
    @BindView(R.id.appbar) AppBarLayout mAppBarLayout;
    @BindView(R.id.toolbar) Toolbar mToolbar;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        ButterKnife.bind(this, mRootView);

        mAppBarLayout.addOnOffsetChangedListener(this);

        bindViews();
        return mRootView;
    }

    @OnClick(R.id.share_fab)
    public void onClickShareFab () {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));
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

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        mBylineView.setMovementMethod(new LinkMovementMethod());
        mBodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            String byLineText;
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                 byLineText = DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                        + "</font>";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mBylineView.setText(Html.fromHtml(byLineText, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    mBylineView.setText(Html.fromHtml(byLineText));
                }

            } else {
                // If date is before 1902, just show the string

                byLineText = outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                        + "</font>";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mBylineView.setText(Html.fromHtml(byLineText, Html.FROM_HTML_MODE_LEGACY));
                } else {
                    mBylineView.setText(Html.fromHtml(byLineText));
                }

            }

            String bodyView = mCursor.getString(ArticleLoader.Query.BODY)
                    .replaceAll("(\r\n|\n)", "<br />");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mBodyView.setText(Html.fromHtml(bodyView, Html.FROM_HTML_MODE_LEGACY));
            } else {
                mBodyView.setText(Html.fromHtml(bodyView));
            }

            Picasso.with(getActivity()).load(mCursor.getString(ArticleLoader.Query.PHOTO_URL)).into(mPhotoView, new Callback() {
                @Override
                public void onSuccess() {
                    mRootView.findViewById(R.id.meta_bar)
                            .setBackgroundColor(mMutedColor);
                }

                @Override
                public void onError() {

                }
            });
        } else {
            mRootView.setVisibility(View.GONE);
            mTitleView.setText("N/A");
            mBylineView.setText("N/A" );
            mBodyView.setText("N/A");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset == 0) {
            if (mAppBarCurrentState != APPBAR_EXPANDED) {
                onStateChanged(APPBAR_EXPANDED);
            }
            mAppBarCurrentState = APPBAR_EXPANDED;
        } else if (Math.abs(verticalOffset) >= appBarLayout.getTotalScrollRange()) {
            if (mAppBarCurrentState != APPBAR_COLLAPSED) {
                onStateChanged(APPBAR_COLLAPSED);
            }
            mAppBarCurrentState = APPBAR_COLLAPSED;
        } else {
            if (mAppBarCurrentState != APPBAR_IDLE) {
                onStateChanged(APPBAR_IDLE);
            }
            mAppBarCurrentState = APPBAR_IDLE;
        }
    }

    private void onStateChanged(String state) {
        switch (state) {
            case APPBAR_EXPANDED:
                disableCollapsingTolbar();
                break;
            case APPBAR_COLLAPSED:
                enableCollapsingToolbar();
                break;
            case APPBAR_IDLE:
                disableCollapsingTolbar();
                break;
        }
    }

    private void disableCollapsingTolbar() {
        mToolbar.setVisibility(View.GONE);
    }

    private void enableCollapsingToolbar() {
        mToolbar.setVisibility(View.VISIBLE);
        mToolbar.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.colorPrimary));
    }
}

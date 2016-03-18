package projekt.dashboard.ui;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.LinearLayout;

import com.afollestad.bridge.Bridge;
import com.afollestad.inquiry.Inquiry;
import com.afollestad.materialdialogs.util.DialogUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import projekt.dashboard.R;
import projekt.dashboard.adapters.MainPagerAdapter;
import projekt.dashboard.config.Config;
import projekt.dashboard.fragments.AboutFragment;
import projekt.dashboard.fragments.ColorChangerFragment;
import projekt.dashboard.fragments.HomeFragment;
import projekt.dashboard.fragments.RequestsFragment;
import projekt.dashboard.fragments.ThemeUtilitiesFragment;
import projekt.dashboard.fragments.WallpapersFragment;
import projekt.dashboard.fragments.base.BasePageFragment;
import projekt.dashboard.ui.base.BaseDonateActivity;
import projekt.dashboard.util.DrawableXmlParser;
import projekt.dashboard.util.PagesBuilder;
import projekt.dashboard.util.TintUtils;
import projekt.dashboard.util.WallpaperUtils;
import projekt.dashboard.views.DisableableViewPager;

import static projekt.dashboard.fragments.WallpapersFragment.RQ_CROPANDSETWALLPAPER;
import static projekt.dashboard.fragments.WallpapersFragment.RQ_VIEWWALLPAPER;
import static projekt.dashboard.viewer.ViewerActivity.STATE_CURRENT_POSITION;

/**
 * @author Aidan Follestad (afollestad)
 */
public class MainActivity extends BaseDonateActivity implements
        NavigationView.OnNavigationItemSelectedListener {

    public RecyclerView mRecyclerView;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Nullable
    @Bind(R.id.tabs)
    TabLayout mTabs;

    @Nullable
    @Bind(R.id.navigation_view)
    NavigationView mNavView;
    @Nullable
    @Bind(R.id.drawer)
    DrawerLayout mDrawer;

    @Bind(R.id.pager)
    DisableableViewPager mPager;

    @Nullable
    @Bind(R.id.app_bar)
    LinearLayout mAppBarLinear;

    int mDrawerModeTopInset;

    private PagesBuilder mPages;

    @Override
    public Toolbar getToolbar() {
        return mToolbar;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final boolean useNavDrawer = Config.get().navDrawerModeEnabled();
        if (useNavDrawer)
            setContentView(R.layout.activity_main_drawer);
        else
            setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        setupPages();
        setupPager();
        if (useNavDrawer)
            setupNavDrawer();
        else
            setupTabs();

        // Restore last selected page, tab/nav-drawer-item
        if (Config.get().persistSelectedPage()) {
            int lastPage = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getInt("last_selected_page", 0);
            if (lastPage > mPager.getAdapter().getCount() - 1) lastPage = 0;
            mPager.setCurrentItem(lastPage);
            if (mNavView != null) invalidateNavViewSelection(lastPage);
        }
        dispatchFragmentUpdateTitle(!useNavDrawer);


        processIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(intent);
    }

    private void processIntent(Intent intent) {
        if (Intent.ACTION_SET_WALLPAPER.equals(intent.getAction())) {
            for (int i = 0; i < mPages.size(); i++) {
                PagesBuilder.Page page = mPages.get(i);
                if (page.drawerId == R.id.drawer_wallpapers) {
                    mPager.setCurrentItem(i);
                    break;
                }
            }
        }
    }

    private void setupPages() {
        mPages = new PagesBuilder(6);
        if (Config.get().homepageEnabled())
            mPages.add(new PagesBuilder.Page(R.id.drawer_home, R.drawable.tab_home, R.string.home_tab_one, new HomeFragment()));
        if (Config.get().colorChangerEnabled())
            mPages.add(new PagesBuilder.Page(R.id.drawer_icons, R.drawable.tab_palette, R.string.home_tab_two, new ColorChangerFragment()));
        if (Config.get().themeRebuilderEnabled())
            mPages.add(new PagesBuilder.Page(R.id.drawer_apply, R.drawable.tab_rebuild, R.string.home_tab_three, new ThemeUtilitiesFragment()));


        if (Config.get().iconRequestEnabled())
            mPages.add(new PagesBuilder.Page(R.id.drawer_requestIcons, R.drawable.tab_requests, R.string.request_icons, new RequestsFragment()));
        if (Config.get().wallpapersEnabled())
            mPages.add(new PagesBuilder.Page(R.id.drawer_wallpapers, R.drawable.tab_wallpapers, R.string.wallpapers, new WallpapersFragment()));


        if (Config.get().aboutEnabled())
            mPages.add(new PagesBuilder.Page(R.id.drawer_about, R.drawable.tab_about, R.string.about, new AboutFragment()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if (!Config.get().changelogEnabled())
            menu.findItem(R.id.changelog).setVisible(false);

        MenuItem darkTheme = menu.findItem(R.id.darkTheme);
        if (!Config.get().allowThemeSwitching())
            darkTheme.setVisible(false);
        else darkTheme.setChecked(darkTheme());

        MenuItem navDrawerMode = menu.findItem(R.id.navDrawerMode);
        if (Config.get().navDrawerModeAllowSwitch()) {
            navDrawerMode.setVisible(true);
            navDrawerMode.setChecked(Config.get().navDrawerModeEnabled());
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.darkTheme) {
            darkTheme(!darkTheme());
            mToolbar.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recreate();
                }
            }, 500);
            return true;
        } else if (item.getItemId() == R.id.navDrawerMode) {
            item.setChecked(!item.isChecked());
            Config.get().navDrawerModeEnabled(item.isChecked());
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupNavDrawer() {
        assert mNavView != null;
        assert mDrawer != null;
        mNavView.getMenu().clear();
        for (PagesBuilder.Page page : mPages)
            page.addToMenu(mNavView.getMenu());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            getWindow().setStatusBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mDrawer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    //TODO: Check if NavigationView needs bottom padding
                    WindowInsets drawerLayoutInsets = insets.replaceSystemWindowInsets(
                            insets.getSystemWindowInsetLeft(),
                            insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(),
                            0
                    );
                    mDrawerModeTopInset = drawerLayoutInsets.getSystemWindowInsetTop();
                    ((DrawerLayout) v).setChildInsets(drawerLayoutInsets,
                            drawerLayoutInsets.getSystemWindowInsetTop() > 0);
                    return insets;
                }
            });
        }

        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Drawable menuIcon = ContextCompat.getDrawable(this, R.drawable.ic_action_menu);
        menuIcon = TintUtils.createTintedDrawable(menuIcon, DialogUtils.resolveColor(this, R.attr.tab_icon_color));
        getSupportActionBar().setHomeAsUpIndicator(menuIcon);

        mDrawer.addDrawerListener(new ActionBarDrawerToggle(this, mDrawer, mToolbar, R.string.drawer_open, R.string.drawer_close));
        mDrawer.setStatusBarBackgroundColor(DialogUtils.resolveColor(this, R.attr.colorPrimaryDark));
        mNavView.setNavigationItemSelectedListener(this);

        final ColorDrawable navBg = (ColorDrawable) mNavView.getBackground();
        final int selectedIconText = DialogUtils.resolveColor(this, R.attr.colorAccent);
        int iconColor;
        int titleColor;
        int selectedBg;
        if (TintUtils.isColorLight(navBg.getColor())) {
            iconColor = ContextCompat.getColor(this, R.color.navigationview_normalicon_light);
            titleColor = ContextCompat.getColor(this, R.color.navigationview_normaltext_light);
            selectedBg = ContextCompat.getColor(this, R.color.navigationview_selectedbg_light);
        } else {
            iconColor = ContextCompat.getColor(this, R.color.navigationview_normalicon_dark);
            titleColor = ContextCompat.getColor(this, R.color.navigationview_normaltext_dark);
            selectedBg = ContextCompat.getColor(this, R.color.navigationview_selectedbg_dark);
        }

        final ColorStateList iconSl = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                new int[]{iconColor, selectedIconText});
        final ColorStateList textSl = new ColorStateList(
                new int[][]{
                        new int[]{-android.R.attr.state_checked},
                        new int[]{android.R.attr.state_checked}
                },
                new int[]{titleColor, selectedIconText});
        mNavView.setItemTextColor(textSl);
        mNavView.setItemIconTintList(iconSl);

        StateListDrawable bgDrawable = new StateListDrawable();
        bgDrawable.addState(new int[]{android.R.attr.state_checked}, new ColorDrawable(selectedBg));
        mNavView.setItemBackground(bgDrawable);

        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                dispatchFragmentUpdateTitle(false);
                invalidateNavViewSelection(position);
            }
        });

        mToolbar.setContentInsetsRelative(getResources().getDimensionPixelSize(R.dimen.second_keyline), 0);
    }

    void invalidateNavViewSelection(int position) {
        assert mNavView != null;
        final int selectedId = mPages.get(position).drawerId;
        mNavView.post(new Runnable() {
            @Override
            public void run() {
                mNavView.setCheckedItem(selectedId);
            }
        });
    }

    @Override
    public int getLastStatusBarInsetHeight() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return 0;
        }

        boolean useNavDrawer = Config.get().navDrawerModeEnabled();
        if (useNavDrawer) {
            return mDrawerModeTopInset;
        } else {
            return findViewById(R.id.root).getPaddingTop();
        }
    }

    private void setupPager() {
        mPager.setAdapter(new MainPagerAdapter(getFragmentManager(), mPages));
        mPager.setOffscreenPageLimit(mPages.size() - 1);
        // Paging is only enabled in tab mode
        mPager.setPagingEnabled(!Config.get().navDrawerModeEnabled());
    }

    private void setupTabs() {
        assert mTabs != null;
        mTabs.setTabMode(mPages.size() > 6 ? TabLayout.MODE_SCROLLABLE : TabLayout.MODE_FIXED);
        mTabs.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mPager));
        mPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabs) {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                dispatchFragmentUpdateTitle(false);
            }
        });

        for (PagesBuilder.Page page : mPages)
            addTab(page.iconRes);
        mTabs.setSelectedTabIndicatorColor(DialogUtils.resolveColor(this, R.attr.tab_indicator_color));
    }

    void dispatchFragmentUpdateTitle(final boolean checkTabsLocation) {
        //First set the presumed title, then let fragment do anything specific.
        setTitle(mPages.get(mPager.getCurrentItem()).titleRes);

        mPager.post(new Runnable() {
            @Override
            public void run() {
                final BasePageFragment frag = (BasePageFragment) getFragmentManager().findFragmentByTag("page:" + mPager.getCurrentItem());
                if (frag != null) frag.updateTitle();

                if (checkTabsLocation) {
                    moveTabsIfNeeded();
                }
            }
        });
    }

    void moveTabsIfNeeded() {
        final CharSequence currentTitle = getTitle();

        String longestTitle = null;
        for (PagesBuilder.Page page : mPages) {
            String title = getString(page.titleRes);
            if (longestTitle == null || title.length() > longestTitle.length()) {
                longestTitle = title;
            }
        }
        setTitle(longestTitle);

        if (mTabs != null) {
            ViewTreeObserver vto = mToolbar.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    if (mToolbar.isTitleTruncated() && mTabs.getParent() == mToolbar) {
                        mToolbar.removeView(mTabs);
                        //noinspection ConstantConditions
                        mAppBarLinear.addView(mTabs);
                    }

                    setTitle(currentTitle);

                    mToolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        ((DrawerLayout) findViewById(R.id.drawer)).closeDrawers();
        final int index = mPages.findPositionForItem(item);
        if (index > -1)
            mPager.setCurrentItem(index, false);
        return false;
    }

    private void addTab(@DrawableRes int icon) {
        assert mTabs != null;
        TabLayout.Tab tab = mTabs.newTab().setIcon(icon);
        if (tab.getIcon() != null) {
            Drawable tintedIcon = DrawableCompat.wrap(tab.getIcon());
            DrawableCompat.setTint(tintedIcon, DialogUtils.resolveColor(this, R.attr.tab_icon_color));
            tab.setIcon(tintedIcon);
        }
        mTabs.addTab(tab);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Config.get().persistSelectedPage()) {
            PreferenceManager.getDefaultSharedPreferences(MainActivity.this)
                    .edit().putInt("last_selected_page", mPager.getCurrentItem()).commit();
        }
        if (isFinishing()) {
            Config.deinit();
            Bridge.destroy();
            Inquiry.deinit();
            DrawableXmlParser.cleanup();
        }
    }

    @Override
    public void onBackPressed() {
        if (mPager != null) {
            FragmentManager fm = getFragmentManager();
        }
        super.onBackPressed();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RQ_CROPANDSETWALLPAPER) {
            WallpapersFragment.showToast(this, R.string.wallpaper_set);
            WallpaperUtils.resetOptionCache(true);
        } else if (requestCode == RQ_VIEWWALLPAPER) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mDrawer != null) {
                getWindow().setStatusBarColor(Color.TRANSPARENT);
                mDrawer.setStatusBarBackgroundColor(DialogUtils.resolveColor(this, R.attr.colorPrimaryDark));
            }
            if (mRecyclerView != null) {
                mRecyclerView.requestFocus();
                final int currentPos = data.getIntExtra(STATE_CURRENT_POSITION, 0);
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecyclerView.smoothScrollToPosition(currentPos);
                    }
                });
            }
        }
    }
}
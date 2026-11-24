/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.auxcamera;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarAppCompatActivity;
import com.android.settingslib.widget.ExpressiveDesignEnabledProvider;

import org.lineageos.settings.R;

public class AuxCameraActivity extends CollapsingToolbarAppCompatActivity
        implements ExpressiveDesignEnabledProvider {

    private static final String TAG_AUX_CAMERA = "aux_camera";

    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private boolean mSearchMode;
    private int mExpandedHeaderHeight;

    private final OnBackPressedCallback mSearchBackCallback =
            new OnBackPressedCallback(false) {
                @Override
                public void handleOnBackPressed() {
                    closeSearch();
                }
            };

    @Override
    public boolean isExpressiveDesignEnabled() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new AuxCameraFragment(), TAG_AUX_CAMERA)
                    .commit();
        }

        getOnBackPressedDispatcher().addCallback(this, mSearchBackCallback);
        setActionButtonEnabled(false);

        final Toolbar toolbar = findToolbar(getWindow().getDecorView());
        if (toolbar != null) {
            toolbar.setCollapseIcon(com.android.settingslib.collapsingtoolbar.R.drawable
                    .settingslib_expressive_icon_back);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.thermal_search_menu, menu);
        mSearchItem = menu.findItem(R.id.action_thermal_search);
        if (mSearchItem == null) {
            return true;
        }

        final Resources res = getResources();
        final String pkg = getPackageName();
        final int onSurface = res.getIdentifier(
                "settingslib_materialColorOnSurface", "color", pkg);
        final int onSurfaceVariant = res.getIdentifier(
                "settingslib_materialColorOnSurfaceVariant", "color", pkg);
        if (onSurfaceVariant != 0) {
            mSearchItem.setIconTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, onSurfaceVariant)));
        }

        mSearchView = (SearchView) mSearchItem.getActionView();
        if (mSearchView != null) {
            mSearchView.setQueryHint(getString(R.string.thermal_search_hint_short));
            mSearchView.setMaxWidth(Integer.MAX_VALUE);

            final EditText edit = mSearchView.findViewById(
                    androidx.appcompat.R.id.search_src_text);
            if (edit != null) {
                if (onSurface != 0) {
                    edit.setTextColor(ContextCompat.getColor(this, onSurface));
                }
                if (onSurfaceVariant != 0) {
                    edit.setHintTextColor(ContextCompat.getColor(this, onSurfaceVariant));
                }
                edit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                edit.setBackground(null);
            }

            final View plate = mSearchView.findViewById(
                    androidx.appcompat.R.id.search_plate);
            if (plate != null) {
                plate.setBackground(null);
            }

            final ImageView closeBtn = mSearchView.findViewById(
                    androidx.appcompat.R.id.search_close_btn);
            if (closeBtn != null && onSurfaceVariant != 0) {
                closeBtn.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, onSurfaceVariant)));
            }

            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    hideKeyboard();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    applyQuery(newText);
                    return true;
                }
            });
        }

        mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchMode = true;
                mSearchBackCallback.setEnabled(true);
                setHeaderCompact(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchMode = false;
                mSearchBackCallback.setEnabled(false);
                setHeaderCompact(false);
                applyQuery("");
                hideKeyboard();
                return true;
            }
        });

        return true;
    }

    private void closeSearch() {
        if (mSearchMode && mSearchItem != null) {
            mSearchItem.collapseActionView();
        }
    }

    private void setHeaderCompact(boolean compact) {
        final View appBar = getAppBarLayout();
        if (appBar == null) {
            return;
        }
        final ViewGroup.LayoutParams lp = appBar.getLayoutParams();
        if (lp == null) {
            return;
        }
        if (compact) {
            if (mExpandedHeaderHeight == 0) {
                mExpandedHeaderHeight = lp.height;
            }
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else if (mExpandedHeaderHeight != 0) {
            lp.height = mExpandedHeaderHeight;
        }
        appBar.setLayoutParams(lp);
    }

    private void applyQuery(String query) {
        Fragment f = getSupportFragmentManager().findFragmentByTag(TAG_AUX_CAMERA);
        if (f instanceof AuxCameraFragment) {
            ((AuxCameraFragment) f).setSearchQuery(query);
        }
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
        }
    }

    private static Toolbar findToolbar(View view) {
        if (view instanceof Toolbar) {
            return (Toolbar) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Toolbar found = findToolbar(group.getChildAt(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}

/*
 * Copyright (C) 2025 kenway214
 * Copyright (C) 2026 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.powertools;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.ExpressiveDesignEnabledProvider;

import org.lineageos.settings.R;

public class PowertoolsActivity extends CollapsingToolbarBaseActivity
        implements ExpressiveDesignEnabledProvider {

    private static final String STATE_SELECTED_TAB = "selected_tab";

    private static final int[] TAB_TITLE_RES = {
            R.string.powertools_tab_general,
            R.string.powertools_tab_cpu,
            R.string.powertools_tab_gpu,
            R.string.powertools_tab_zram,
            R.string.powertools_tab_io,
            R.string.powertools_tab_net,
    };

    private HorizontalScrollView mTabsScroll;
    private LinearLayout mTabsBar;
    private final TextView[] mTabViews = new TextView[TAB_TITLE_RES.length];
    private int mSelectedTab = PowertoolsSettingsFragment.TAB_GENERAL;

    @Override
    public boolean isExpressiveDesignEnabled() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.powertools_tabs_container);

        mTabsScroll = findViewById(R.id.powertools_tabs_scroll);
        mTabsBar = findViewById(R.id.powertools_tabs_bar);

        if (savedInstanceState != null) {
            mSelectedTab = savedInstanceState.getInt(
                    STATE_SELECTED_TAB, PowertoolsSettingsFragment.TAB_GENERAL);
        }

        buildTabBar();
        selectTab(mSelectedTab, savedInstanceState == null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, mSelectedTab);
    }

    private void buildTabBar() {
        int containerColor = resolveMonetColor(
                "settingslib_materialColorSurfaceContainer", 0xFF141618);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(containerColor);
        barBg.setCornerRadius(dp(22));
        mTabsBar.setBackground(barBg);

        mTabsBar.removeAllViews();
        for (int i = 0; i < TAB_TITLE_RES.length; i++) {
            final int tabIndex = i;
            TextView tv = new TextView(this);
            tv.setText(TAB_TITLE_RES[i]);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            tv.setGravity(Gravity.CENTER);
            tv.setSingleLine(true);
            int hPad = dp(16);
            tv.setPadding(hPad, 0, hPad, 0);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            lp.setMargins(dp(2), 0, dp(2), 0);
            tv.setLayoutParams(lp);
            tv.setMinWidth(dp(68));

            tv.setOnClickListener(v -> selectTab(tabIndex, true));
            mTabsBar.addView(tv);
            mTabViews[i] = tv;
        }
    }

    private void selectTab(int tabIndex, boolean replaceFragment) {
        if (tabIndex < 0 || tabIndex >= TAB_TITLE_RES.length) {
            tabIndex = PowertoolsSettingsFragment.TAB_GENERAL;
        }
        mSelectedTab = tabIndex;

        int activeBgColor = resolveMonetColor(
                "settingslib_materialColorSecondaryContainer", 0xFF26424C);
        int activeFgColor = resolveMonetColor(
                "settingslib_materialColorOnSecondaryContainer", 0xFFBCE9F8);
        int inactiveFgColor = resolveMonetColor(
                "settingslib_materialColorOnSurfaceVariant", 0xFFA5ACB2);

        for (int i = 0; i < mTabViews.length; i++) {
            TextView tv = mTabViews[i];
            if (tv == null) {
                continue;
            }
            if (i == mSelectedTab) {
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(activeBgColor);
                pill.setCornerRadius(dp(18));
                tv.setBackground(pill);
                tv.setTextColor(activeFgColor);
                tv.setTypeface(null, Typeface.BOLD);
            } else {
                tv.setBackground(null);
                tv.setTextColor(inactiveFgColor);
                tv.setTypeface(null, Typeface.NORMAL);
            }
        }

        if (mTabsScroll != null && mTabViews[mSelectedTab] != null) {
            final View activeView = mTabViews[mSelectedTab];
            mTabsScroll.post(() -> {
                int scrollX = activeView.getLeft()
                        - (mTabsScroll.getWidth() - activeView.getWidth()) / 2;
                mTabsScroll.smoothScrollTo(Math.max(0, scrollX), 0);
            });
        }

        if (replaceFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.powertools_tab_content,
                            PowertoolsSettingsFragment.newInstance(mSelectedTab),
                            "powertools_tab_" + mSelectedTab)
                    .commit();
        }
    }

    private int resolveMonetColor(String resName, int fallback) {
        Resources res = getResources();
        int id = res.getIdentifier(resName, "color", getPackageName());
        if (id != 0) {
            return ContextCompat.getColor(this, id);
        }
        return fallback;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

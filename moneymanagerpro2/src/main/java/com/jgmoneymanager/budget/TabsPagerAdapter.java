package com.jgmoneymanager.budget;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

public class TabsPagerAdapter extends FragmentPagerAdapter {

    private BudgetStatus budgetStatus = new BudgetStatus();
    private BudgetCategories budgetCategories = new BudgetCategories();

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int index) {

        switch (index) {
            case 0:
                return budgetStatus;
            case 1:
                return budgetCategories;
        }

        return null;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);
        // save the appropriate reference depending on position
        switch (position) {
            case 0:
                budgetStatus = (BudgetStatus) createdFragment;
                break;
            case 1:
                budgetCategories = (BudgetCategories) createdFragment;
                break;
        }
        return createdFragment;
    }

    @Override
    public int getCount() {
        // get item count - equal to number of tabs
        return 2;
    }

    public BudgetStatus getBudgetStatus() {
        return budgetStatus;
    }

    public BudgetCategories getBudgetCategories() {
        return budgetCategories;
    }

}
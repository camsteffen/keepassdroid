package com.keepassdroid.database;

import android.support.v4.app.Fragment;

public abstract class PwItem {
    public abstract Fragment createFragment();

    public abstract CharSequence getName();
}

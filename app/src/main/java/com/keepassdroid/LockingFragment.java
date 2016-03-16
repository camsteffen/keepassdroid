package com.keepassdroid;

import android.support.v4.app.Fragment;
import com.keepassdroid.timeout.TimeoutHelper;

public class LockingFragment extends Fragment {
    @Override
    public void onPause() {
        super.onPause();

        TimeoutHelper.pause(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();

        TimeoutHelper.resume(getActivity());
    }
}

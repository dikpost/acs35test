package ru.dikpost.acs35test.preference;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by oracle on 2/23/16.
 */
public class ListPreference extends android.preference.ListPreference {

    public ListPreference(Context context) {
        this(context, null);
    }

    public ListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            setSummary(getSummary());
        }
    }

    @Override
    public CharSequence getSummary() {
        int pos = findIndexOfValue(getValue());
        return getEntries()[pos];
    }
}

package com.akylas.enforcedoze;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;

/* Created by Faiz Visram on 2014-03-11 */

public class NumberPickerPreference extends Preference implements
        Preference.OnPreferenceClickListener, DialogInterface.OnClickListener, View.OnClickListener {
    private final boolean DEFAULT_BIND_SUMMARY = true;
    private final int DEFAULT_MAX = 10;
    private final int DEFAULT_MIN = 0;
    private final int DEFAULT_STEP = 1;

    AlertDialog mDialog;
    NumberPicker mPicker;
    String mTitle;
    boolean mBindSummary = DEFAULT_BIND_SUMMARY;
    int mMax = DEFAULT_MAX;
    int mMin = DEFAULT_MIN;
    int mStep = DEFAULT_STEP;
    int mCurrentValue = mMin;
    String[] mValues;

    public NumberPickerPreference(Context context) {
        super(context);
        init(context, null);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        setOnPreferenceClickListener(this);

        if (attrs != null) {
            int title = attrs.getAttributeResourceValue("http://schemas.android.com/apk/res/android", "title", -1);
            if (title != -1) {
                mTitle = context.getString(title);
            }
            mBindSummary = attrs.getAttributeBooleanValue(null, "bindSummary", DEFAULT_BIND_SUMMARY);
            mMin = attrs.getAttributeIntValue(null, "min", DEFAULT_MIN);
            mMax = attrs.getAttributeIntValue(null, "max", DEFAULT_MAX);
            mStep = attrs.getAttributeIntValue(null, "step", DEFAULT_STEP);
            mCurrentValue = mMin;
        }

        if (mTitle == null) {
            mTitle = "Choose delay (minutes)";
        }
        if (mMax < mMin) {
            throw new AssertionError("max value must be > min value");
        }
        if (mStep <= 0) {
            throw new AssertionError("step value must be > 0");
        }

    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            mCurrentValue = this.getPersistedInt(mMin);
        } else {
            // Set default state from the XML attribute
            mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }
        Log.e("NumberPickerPreference", "mCurrentValue: " + mCurrentValue);
        if (mBindSummary) {
            setSummary(Integer.toString(mCurrentValue));
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        // Check whether this Preference is persistent (continually saved)
        if (isPersistent()) {
            // No need to save instance state since it's persistent, use superclass state
            return superState;
        }

        // Create instance of custom BaseSavedState
        final SavedState myState = new SavedState(superState);
        // Set the state's value with the class member that holds current setting value
        myState.value = mCurrentValue;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        // Check whether we saved the state in onSaveInstanceState
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save the state, so call superclass
            super.onRestoreInstanceState(state);
            return;
        }

        // Cast state to custom BaseSavedState and pass to superclass
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());

        // Set this Preference's widget to reflect the restored state
        mPicker.setValue(myState.value);
        mCurrentValue = myState.value;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        showDialog();
        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void showDialog() {
        if (mDialog == null) {

            View view = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.view_number_picker_dialog, null);

            // build NumberPicker
            mPicker = (NumberPicker) view.findViewById(R.id.number_picker);
            if (mStep > 1) {
                mValues = new String[(mMax - mMin) / mStep + 1];
                for (int i = 0; i < mValues.length; i++) {
                    mValues[i] = Integer.toString(mMin + mStep * i);
                }
                mPicker.setMinValue(0);
                mPicker.setMaxValue((mMax - mMin) / mStep);
                mPicker.setDisplayedValues(mValues);
            } else {
                mPicker.setMaxValue(mMax);
                mPicker.setMinValue(mMin);
                mPicker.setValue(mCurrentValue);
            }

            // build save button
            Button saveButton = (Button) view.findViewById(R.id.btn_save);
            saveButton.setOnClickListener(this);

            // build dialog
            mDialog = new AlertDialog.Builder(getContext())
                    .setTitle(mTitle)
                    .setView(view)
                    .setCancelable(true)
                    .create();

            mDialog.setCanceledOnTouchOutside(true);
        }

        mDialog.show();
    }

    private void save(int value) {
        if (mBindSummary) {
            setSummary(Integer.toString(value));
        }
        if (getOnPreferenceChangeListener() != null) {
            getOnPreferenceChangeListener().onPreferenceChange(this, value);
        }
        persistInt(value);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (AlertDialog.BUTTON_POSITIVE == which) {
            save(getValue());
        }
    }

    public int getValue() {
        if (mStep == 1) {
            return mPicker.getValue();
        } else {
            return Integer.parseInt(mValues[mPicker.getValue()]);
        }

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onClick(View v) {
        save(getValue());
        mDialog.dismiss();
    }

    private static class SavedState extends BaseSavedState {
        // Member that holds the setting's value
        int value;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            // Get the current preference's value
            value = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            // Write the preference's value
            dest.writeInt(value);
        }

        // Standard creator object using an instance of this class
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
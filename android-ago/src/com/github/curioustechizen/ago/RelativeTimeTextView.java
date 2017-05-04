
package com.github.curioustechizen.ago;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.icu.text.RelativeDateTimeFormatter.AbsoluteUnit.NOW;
import static com.github.curioustechizen.ago.RelativeTimeTextView.RelativeTimeFormat.RTF_ONLY_TIME_RELATIVE;

/**
 * A {@code TextView} that, given a reference time, renders that time as a time period relative to the current time.
 *
 * @author Kiran Rao
 * @see #setReferenceTime(RelativeTimeFormat, long)
 */
public class RelativeTimeTextView extends TextView {

    public enum RelativeTimeFormat {
        RTF_ONLY_DATE_RELATIVE,
        RTF_ONLY_DATE_ABSOLUTE,
        RTF_ONLY_TIME_RELATIVE,
        RTF_ONLY_TIME_RELATIVE_TODAY_AND_ABSOLUTE_OTHERWISE,
        RTF_ONLY_TIME_ABSOLUTE,
        RTF_COMPOSED_ABSOLUTE,
        RTF_COMPOSED_ABSOLUTE_OMITTING_TODAY,
        RTF_COMPOSED_RELATIVE_OMITTING_TODAY,
        RTF_COMPOSED_RELATIVE_OMITTING_TODAY_WITH_COUNTER;
    }

    private static final long INITIAL_UPDATE_INTERVAL = DateUtils.MINUTE_IN_MILLIS;

    private RelativeTimeFormat mTimeFormat;
    private long mReferenceTime;
    private String mText;
    private String mPrefix;
    private String mSuffix;
    private Handler mHandler = new Handler();
    private UpdateTimeRunnable mUpdateTimeTask;
    private boolean isUpdateTaskRunning = false;
    private static String mNowString = "Now";

    public RelativeTimeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RelativeTimeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.RelativeTimeTextView, 0, 0);
        try {
            mText = a.getString(R.styleable.RelativeTimeTextView_reference_time);
            mPrefix = a.getString(R.styleable.RelativeTimeTextView_relative_time_prefix);
            mSuffix = a.getString(R.styleable.RelativeTimeTextView_relative_time_suffix);

            mPrefix = mPrefix == null ? "" : mPrefix;
            mSuffix = mSuffix == null ? "" : mSuffix;
        } finally {
            a.recycle();
        }

        try {
            mReferenceTime = Long.valueOf(mText);
        } catch (NumberFormatException nfe) {
            /*
             * TODO: Better exception handling
        	 */
            mReferenceTime = -1L;
        }

        setReferenceTime(RTF_ONLY_TIME_RELATIVE, mReferenceTime);
    }

    private static String getNowString() {
        return RelativeTimeTextView.mNowString;
    }

    public static void setNowString(String mNowString) {
        RelativeTimeTextView.mNowString = mNowString;
    }

    /**
     * Returns prefix
     *
     * @return
     */
    public String getPrefix() {
        return this.mPrefix;
    }

    /**
     * String to be attached before the reference time
     *
     * @param prefix Example:
     *               [prefix] in XX minutes
     */
    public void setPrefix(String prefix) {
        this.mPrefix = prefix;
        updateTextDisplay();
    }

    /**
     * Returns suffix
     *
     * @return
     */
    public String getSuffix() {
        return this.mSuffix;
    }

    /**
     * String to be attached after the reference time
     *
     * @param suffix Example:
     *               in XX minutes [suffix]
     */
    public void setSuffix(String suffix) {
        this.mSuffix = suffix;
        updateTextDisplay();
    }

    /**
     * Sets the reference time for this view. At any moment, the view will render a relative time period relative to the time set here.
     * <p/>
     * This value can also be set with the XML attribute {@code reference_time}
     *
     * @param referenceTime The timestamp (in milliseconds since epoch) that will be the reference point for this view.
     */
    public void setReferenceTime(RelativeTimeFormat timeFormat, long referenceTime) {
        this.mTimeFormat = timeFormat;
        this.mReferenceTime = referenceTime;
        
        /*
         * Note that this method could be called when a row in a ListView is recycled.
         * Hence, we need to first stop any currently running schedules (for example from the recycled view.
         */
        stopTaskForPeriodicallyUpdatingRelativeTime();
        
        /*
         * Instantiate a new runnable with the new reference time
         */
        initUpdateTimeTask();
        
        /*
         * Start a new schedule.
         */
        startTaskForPeriodicallyUpdatingRelativeTime();
        
        /*
         * Finally, update the text display.
         */
        updateTextDisplay();
    }

    private void updateTextDisplay() {
        /*
         * TODO: Validation, Better handling of negative cases
         */
        if (this.mReferenceTime == -1L)
            return;
        setText(mPrefix + RelativeTimeTextView.getFormattedDateAndTime(mTimeFormat, mReferenceTime) + mSuffix);
    }


    //OnlyDateRelative —> Today/tomorrow
    //OnlyDateAbsolute —> 16 Jul 2015
    //OnlyTimeRelative —> Now, In 5m, 6m ago
    //OnlyTimeAbsolute —> 16:40
    //ComposedAbsoluteOmittingToday —> 16:50 1 Jul 2015 || 16:50
    //ComposedRelativeOmittingToday —> Tomorrow 10:15 || 10:50 (in 10m)
    //ComposedRelativeOmittingTodayWithCounter —> Tomorrow 10:15 || 10:50 (in 10m)

    public static CharSequence getFormattedDateAndTime(RelativeTimeFormat mTimeFormat, long mReferenceTime) {

        CharSequence composed = "";

        switch (mTimeFormat) {
            case RTF_ONLY_DATE_RELATIVE:
                composed = getFormattedDate(mTimeFormat, mReferenceTime);
                break;
            case RTF_ONLY_DATE_ABSOLUTE:
                composed = getFormattedDate(mTimeFormat, mReferenceTime);
                break;
            case RTF_ONLY_TIME_RELATIVE:
                composed = getFormattedTime(mTimeFormat, mReferenceTime);
                break;
            case RTF_ONLY_TIME_RELATIVE_TODAY_AND_ABSOLUTE_OTHERWISE:
                if (DateUtils.isToday(mReferenceTime)) {
                    composed = getFormattedTime(mTimeFormat, mReferenceTime);
                } else {
                    composed = getSimpleFormattedTime(mReferenceTime);
                }
                break;
            case RTF_ONLY_TIME_ABSOLUTE:
                composed = getSimpleFormattedTime(mReferenceTime);

                break;
            case RTF_COMPOSED_ABSOLUTE: {
                // Time
                CharSequence timeStr = getSimpleFormattedTime(mReferenceTime);

                // Date
                CharSequence dateStr = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(mReferenceTime));
                composed = timeStr + " " + dateStr;
                break;
            }
            case RTF_COMPOSED_ABSOLUTE_OMITTING_TODAY: {
                // Time
                composed = getSimpleFormattedTime(mReferenceTime);
                // Date
                if (!DateUtils.isToday(mReferenceTime)) {
                    CharSequence dateStr = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(mReferenceTime));
                    composed = composed + " " + dateStr;
                }
                break;
            }
            case RTF_COMPOSED_RELATIVE_OMITTING_TODAY: {
                // Time
                composed = getSimpleFormattedTime(mReferenceTime);
                // Date
                if (!DateUtils.isToday(mReferenceTime)) {
                    composed = getFormattedDate(mTimeFormat, mReferenceTime) + " " + composed;
                }
                break;
            }
            case RTF_COMPOSED_RELATIVE_OMITTING_TODAY_WITH_COUNTER: {
                // Time
                composed = getSimpleFormattedTime(mReferenceTime);

                // Date
                if (!DateUtils.isToday(mReferenceTime)) {
                    composed = getFormattedDate(mTimeFormat, mReferenceTime) + " " + composed;
                }
                else {
                    composed = composed + " (" + getFormattedTime(mTimeFormat, mReferenceTime) + ")";
                }
                break;
            }
            default:
                break;
        }

        return composed;
    }

    private static boolean isTomorrow(long timeInMillis) {
        return DateUtils.isToday(new Date(timeInMillis).getTime() - DateUtils.DAY_IN_MILLIS);
    }

    private static boolean isYesterday(long timeInMillis) {
        return DateUtils.isToday(new Date(timeInMillis).getTime() + DateUtils.DAY_IN_MILLIS);
    }

    private static boolean isWithinThreeNextDays(long timeInMillis) {
        return DateUtils.isToday(new Date(timeInMillis).getTime() - 2 * DateUtils.DAY_IN_MILLIS)
                || DateUtils.isToday(new Date(timeInMillis).getTime() - 3 * DateUtils.DAY_IN_MILLIS);
    }

    private static boolean isNow(long timeInMillis) {
        return timeInMillis > System.currentTimeMillis() - 30 * 1000 && timeInMillis < System.currentTimeMillis() + 30 * 1000;
    }

    private static boolean isWithin24H(long timeInMillis) {
        return timeInMillis > System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS && timeInMillis < System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS;
    }

    private static CharSequence getFormattedDate(RelativeTimeFormat mTimeFormat, long mReferenceTime) {

        CharSequence result = "";

        switch (mTimeFormat) {
            case RTF_ONLY_DATE_RELATIVE:
            case RTF_COMPOSED_RELATIVE_OMITTING_TODAY:
                if (DateUtils.isToday(mReferenceTime)
                        || isTomorrow(mReferenceTime)
                        || isYesterday(mReferenceTime)) {
                    result = DateUtils.getRelativeTimeSpanString(
                            mReferenceTime,
                            System.currentTimeMillis(),
                            DateUtils.DAY_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE);
                } else if (isWithinThreeNextDays(mReferenceTime)) {
                    SimpleDateFormat format = new SimpleDateFormat("EEEE");
                    result = format.format(new Date(mReferenceTime));
                } else {
                    SimpleDateFormat format = new SimpleDateFormat("EEE dd, MMM");
                    result = format.format(new Date(mReferenceTime));
                }
                break;
            case RTF_ONLY_DATE_ABSOLUTE:
                result = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(mReferenceTime));
                break;
        }

        return result;
    }

    private static CharSequence getFormattedTime(RelativeTimeFormat mTimeFormat, long mReferenceTime) {
        CharSequence result = "";

        switch (mTimeFormat) {
            case RTF_ONLY_TIME_RELATIVE:
            case RTF_ONLY_TIME_RELATIVE_TODAY_AND_ABSOLUTE_OTHERWISE:
            case RTF_COMPOSED_RELATIVE_OMITTING_TODAY_WITH_COUNTER:
                if (isNow(mReferenceTime)) {
                    result = getNowString();
                } else if (isWithin24H(mReferenceTime)) {
                    result = DateUtils.getRelativeTimeSpanString(
                            mReferenceTime,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE);
                } else {
                    SimpleDateFormat format = new SimpleDateFormat("EEE dd, MMM");
                    result = format.format(new Date(mReferenceTime));
                }
                break;
            case RTF_ONLY_DATE_ABSOLUTE:
                result = DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(mReferenceTime));
                break;
        }

        return result;
    }

    private static CharSequence getSimpleFormattedTime(long mReferenceTime) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        return format.format(new Date(mReferenceTime));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startTaskForPeriodicallyUpdatingRelativeTime();

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopTaskForPeriodicallyUpdatingRelativeTime();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == GONE || visibility == INVISIBLE) {
            stopTaskForPeriodicallyUpdatingRelativeTime();
        } else {
            startTaskForPeriodicallyUpdatingRelativeTime();
        }
    }

    private void startTaskForPeriodicallyUpdatingRelativeTime() {
        if (mUpdateTimeTask.isDetached()) initUpdateTimeTask();
        mHandler.post(mUpdateTimeTask);
        isUpdateTaskRunning = true;
    }

    private void initUpdateTimeTask() {
        mUpdateTimeTask = new UpdateTimeRunnable(this, mReferenceTime);
    }

    private void stopTaskForPeriodicallyUpdatingRelativeTime() {
        if (isUpdateTaskRunning) {
            mUpdateTimeTask.detach();
            mHandler.removeCallbacks(mUpdateTimeTask);
            isUpdateTaskRunning = false;
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.referenceTime = mReferenceTime;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        mReferenceTime = ss.referenceTime;
        super.onRestoreInstanceState(ss.getSuperState());
    }

    public static class SavedState extends BaseSavedState {

        private long referenceTime;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeLong(referenceTime);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private SavedState(Parcel in) {
            super(in);
            referenceTime = in.readLong();
        }
    }

    private static class UpdateTimeRunnable implements Runnable {

        private long mRefTime;
        private final WeakReference<RelativeTimeTextView> weakRefRttv;

        UpdateTimeRunnable(RelativeTimeTextView rttv, long refTime) {
            this.mRefTime = refTime;
            weakRefRttv = new WeakReference<>(rttv);
        }

        boolean isDetached() {
            return weakRefRttv.get() == null;
        }

        void detach() {
            weakRefRttv.clear();
        }

        @Override
        public void run() {
            RelativeTimeTextView rttv = weakRefRttv.get();
            if (rttv == null) return;
            long difference = Math.abs(System.currentTimeMillis() - mRefTime);
            long interval = INITIAL_UPDATE_INTERVAL;
            if (difference > DateUtils.WEEK_IN_MILLIS) {
                interval = DateUtils.WEEK_IN_MILLIS;
            } else if (difference > DateUtils.DAY_IN_MILLIS) {
                interval = DateUtils.DAY_IN_MILLIS;
            } else if (difference > DateUtils.HOUR_IN_MILLIS) {
                interval = DateUtils.HOUR_IN_MILLIS;
            }
            rttv.updateTextDisplay();
            rttv.mHandler.postDelayed(this, interval);

        }
    }
}

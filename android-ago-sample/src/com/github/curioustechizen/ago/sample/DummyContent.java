package com.github.curioustechizen.ago.sample;

import android.text.format.DateUtils;

import com.github.curioustechizen.ago.RelativeTimeTextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.github.curioustechizen.ago.RelativeTimeTextView.RelativeTimeFormat.*;

public class DummyContent {

    private static final long NOW = new Date().getTime();

    public static class RowItem {
        public String info;
        public long timestamp;
        public RelativeTimeTextView.RelativeTimeFormat format;

        RowItem(String info, long timestamp, RelativeTimeTextView.RelativeTimeFormat format) {
            this.info = info;
            this.timestamp = timestamp;
            this.format = format;
        }
    }

    public static final List<RowItem> DUMMY_ITEMS = new ArrayList<DummyContent.RowItem>(4);

    static {
//        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_DATE_RELATIVE --> Yesterday", NOW - (60 * 60 * 24 * 1000), RTF_ONLY_DATE_RELATIVE));
//        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_DATE_RELATIVE --> Today", NOW, RTF_ONLY_DATE_RELATIVE));
//        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_DATE_RELATIVE --> Tomorrow", NOW + (60 * 60 * 24 * 1000), RTF_ONLY_DATE_RELATIVE));
//        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_DATE_RELATIVE --> The day after", NOW + (2 * 60 * 60 * 24 * 1000), RTF_ONLY_DATE_RELATIVE));
//        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_DATE_RELATIVE --> Three days after", NOW + (4 * 60 * 60 * 24 * 1000), RTF_ONLY_DATE_RELATIVE));

        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_TIME_RELATIVE --> 5 min ago", NOW - 5 * DateUtils.MINUTE_IN_MILLIS, RTF_ONLY_TIME_RELATIVE));
        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_TIME_RELATIVE --> Now", NOW, RTF_ONLY_TIME_RELATIVE));
        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_TIME_RELATIVE --> In 5 min", NOW + 5 * DateUtils.MINUTE_IN_MILLIS, RTF_ONLY_TIME_RELATIVE));
        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_TIME_RELATIVE --> In 2h 30min", NOW + 2 * DateUtils.HOUR_IN_MILLIS + 30 * DateUtils.MINUTE_IN_MILLIS, RTF_ONLY_TIME_RELATIVE));
        DUMMY_ITEMS.add(new RowItem("RTF_ONLY_TIME_RELATIVE --> In 2days", NOW + 2 * DateUtils.DAY_IN_MILLIS, RTF_ONLY_TIME_RELATIVE));


//		DUMMY_ITEMS.add(new RowItem("Another message", NOW - (15 * 60 * 1000)));
//		DUMMY_ITEMS.add(new RowItem("Not-so-recent message", NOW - (24 * 60 * 60 * 1000)));
//		DUMMY_ITEMS.add(new RowItem("Very old message", NOW - (8 * 24 * 60 * 60 * 1000)));
//		DUMMY_ITEMS.add(new RowItem("Near future message", NOW + (4 * 60 * 1000)));
//		DUMMY_ITEMS.add(new RowItem("Another future message", NOW + (25 * 60 * 60 * 1000)));
    }

}

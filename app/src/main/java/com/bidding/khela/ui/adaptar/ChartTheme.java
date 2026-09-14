package com.bidding.khela.ui.adaptar;

import android.graphics.drawable.GradientDrawable;

import com.bidding.khela.database.EntryJDO;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ChartTheme {

    public static final String HEADER_TRANSITION = "khela_chart_header";

    public static final ChartTheme[] THEMES = new ChartTheme[]{
            new ChartTheme(0xFFFF4D8D, 0xFFC2185B, 0xFFFFF0F5, 0xFFF3C1D4, 0xFFC2185B, 0xFFF8BBD0, 0xFF6D214F, 0xFFE8A0C0),
            new ChartTheme(0xFFFF6B4A, 0xFFD84315, 0xFFFFF4E8, 0xFFFFD7C2, 0xFFE53935, 0xFFFFCCBC, 0xFF8D2B16, 0xFFE0A090),
            new ChartTheme(0xFFF5C518, 0xFFC49000, 0xFFFFF9E6, 0xFFF8EBB0, 0xFFC9A227, 0xFFF5E6B8, 0xFF6B5A1E, 0xFFD4C48A),
            new ChartTheme(0xFF7ED321, 0xFF2E9E4F, 0xFFF1FAE8, 0xFFD4F0C8, 0xFF2E7D32, 0xFFC8E6C9, 0xFF1B5E20, 0xFFA5D6A7),
            new ChartTheme(0xFF2F80ED, 0xFF1565C0, 0xFFEAF4FF, 0xFFCDE6FB, 0xFF1565C0, 0xFFBBDEFB, 0xFF0D47A1, 0xFF90CAF9),
            new ChartTheme(0xFF9B51E0, 0xFF6A1B9A, 0xFFF6EEFF, 0xFFE6CFF5, 0xFF7B1FA2, 0xFFE1BEE7, 0xFF4A148C, 0xFFCE93D8)
    };

    public final int headerStart;
    public final int headerEnd;
    public final int body;
    public final int chip;
    public final int badge;
    public final int emptyBadge;
    public final int pattiFilled;
    public final int pattiEmpty;

    ChartTheme(int headerStart, int headerEnd, int body, int chip, int badge, int emptyBadge, int pattiFilled, int pattiEmpty) {
        this.headerStart = headerStart;
        this.headerEnd = headerEnd;
        this.body = body;
        this.chip = chip;
        this.badge = badge;
        this.emptyBadge = emptyBadge;
        this.pattiFilled = pattiFilled;
        this.pattiEmpty = pattiEmpty;
    }

    public static ChartTheme at(int index) {
        int size = THEMES.length;
        int safeIndex = ((index % size) + size) % size;
        return THEMES[safeIndex];
    }

    public GradientDrawable headerDrawable() {
        return new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{headerStart, headerEnd}
        );
    }

    public GradientDrawable roundedBox(int color, float radiusPx) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    public static String weekdayName(EntryJDO entry) {
        try {
            Date date;
            if (entry.getDateLong() > 0) {
                date = new Date(entry.getDateLong());
            } else {
                date = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).parse(entry.getDateString());
            }
            if (date == null) {
                return "";
            }
            return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date);
        } catch (Exception ignored) {
            return "";
        }
    }
}

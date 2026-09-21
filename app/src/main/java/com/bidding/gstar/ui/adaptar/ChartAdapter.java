package com.bidding.gstar.ui.adaptar;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.bidding.gstar.R;
import com.bidding.gstar.database.EntryJDO;
import com.bidding.gstar.database.Helper;

import java.util.List;
import java.util.Locale;

public class ChartAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final List<EntryJDO> entryList;
    private final Helper helper = new Helper();
    private boolean showSingles = true;

    public ChartAdapter(Context context, List<EntryJDO> pEntryList) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        entryList = pEntryList;
    }

    public void setShowSingles(boolean showSingles) {
        this.showSingles = showSingles;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int index, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.referral_item_layout, parent, false);
            holder = new ViewHolder();
            holder.card = (CardView) convertView;
            holder.dateHeaderBar = convertView.findViewById(R.id.dateHeaderBar);
            holder.numbersArea = convertView.findViewById(R.id.numbersArea);
            holder.mDateHeader = convertView.findViewById(R.id.todayDate);
            holder.weekday = convertView.findViewById(R.id.weekday);
            holder.chipViews = new View[]{
                    convertView.findViewById(R.id.chip_1),
                    convertView.findViewById(R.id.chip_2),
                    convertView.findViewById(R.id.chip_3),
                    convertView.findViewById(R.id.chip_4),
                    convertView.findViewById(R.id.chip_5),
                    convertView.findViewById(R.id.chip_6),
                    convertView.findViewById(R.id.chip_7),
                    convertView.findViewById(R.id.chip_8)
            };
            holder.pattiViews = new TextView[]{
                    convertView.findViewById(R.id.ref_head_1),
                    convertView.findViewById(R.id.ref_head_2),
                    convertView.findViewById(R.id.ref_head_3),
                    convertView.findViewById(R.id.ref_head_4),
                    convertView.findViewById(R.id.ref_head_5),
                    convertView.findViewById(R.id.ref_head_6),
                    convertView.findViewById(R.id.ref_head_7),
                    convertView.findViewById(R.id.ref_head_8)
            };
            holder.singleViews = new TextView[]{
                    convertView.findViewById(R.id.ref_1),
                    convertView.findViewById(R.id.ref_2),
                    convertView.findViewById(R.id.ref_3),
                    convertView.findViewById(R.id.ref_4),
                    convertView.findViewById(R.id.ref_5),
                    convertView.findViewById(R.id.ref_6),
                    convertView.findViewById(R.id.ref_7),
                    convertView.findViewById(R.id.ref_8)
            };
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ChartTheme theme = ChartTheme.at(index);
        EntryJDO entry = entryList.get(index);
        holder.mDateHeader.setText(entry.getDateString());
        holder.weekday.setText(ChartTheme.weekdayName(entry));
        holder.card.setCardBackgroundColor(theme.body);
        holder.numbersArea.setBackgroundColor(theme.body);
        holder.dateHeaderBar.setBackground(theme.headerDrawable());
        convertView.setContentDescription(
                mContext.getString(R.string.khela_chart_row_content_description, entry.getDateString())
        );

        List<?> values = entry.getEntry();
        int singleVisibility = showSingles ? View.VISIBLE : View.GONE;
        for (int slot = 0; slot < holder.pattiViews.length; slot++) {
            holder.chipViews[slot].setBackground(theme.roundedBox(theme.chip, dp(14)));
            holder.singleViews[slot].setVisibility(singleVisibility);
            bindSlot(holder.pattiViews[slot], holder.singleViews[slot], values, slot, theme);
        }
        return convertView;
    }

    private void bindSlot(TextView pattiView, TextView singleView, List<?> values, int slot, ChartTheme theme) {
        String empty = mContext.getString(R.string.khela_chart_empty_slot);
        boolean hasValue = values != null && values.size() > slot && values.get(slot) != null;
        if (hasValue) {
            String value = String.valueOf(values.get(slot));
            pattiView.setText(value);
            pattiView.setTextColor(theme.pattiFilled);
            singleView.setText(toSingleDigit(value));
            singleView.setTextColor(0xFFFFFFFF);
            singleView.setBackground(theme.roundedBox(theme.badge, dp(10)));
        } else {
            pattiView.setText(empty);
            pattiView.setTextColor(theme.pattiEmpty);
            singleView.setText(empty);
            singleView.setTextColor(theme.pattiEmpty);
            singleView.setBackground(theme.roundedBox(theme.emptyBadge, dp(10)));
        }
    }

    private String toSingleDigit(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return mContext.getString(R.string.khela_chart_empty_slot);
        }
        int number = Integer.parseInt(digits);
        String padded = String.format(Locale.US, "%03d", number % 1000);
        return helper.convertToPattiValue(padded);
    }

    private float dp(float value) {
        return value * mContext.getResources().getDisplayMetrics().density;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public String getItem(int position) {
        return entryList.get(position).getDateString();
    }

    @Override
    public int getCount() {
        return entryList.size();
    }

    static class ViewHolder {
        CardView card;
        View dateHeaderBar;
        View numbersArea;
        TextView mDateHeader;
        TextView weekday;
        TextView[] pattiViews;
        TextView[] singleViews;
        View[] chipViews;
    }
}

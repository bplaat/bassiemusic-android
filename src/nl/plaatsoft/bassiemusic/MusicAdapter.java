package nl.plaatsoft.bassiemusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MusicAdapter extends ArrayAdapter<Music> {
    private static class ViewHolder {
        public TextView musicTitle;
        public TextView musicDuration;
    }

    private int selectedPosition = -1;

    public MusicAdapter(Context context) {
        super(context, 0);
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
        notifyDataSetChanged();
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_music, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.musicTitle = (TextView)convertView.findViewById(R.id.music_title);
            viewHolder.musicDuration = (TextView)convertView.findViewById(R.id.music_duration);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        if (position == selectedPosition) {
            convertView.setBackgroundResource(R.color.selected_background_color);
        } else {
            convertView.setBackgroundResource(0);
        }

        Music music = getItem(position);

        viewHolder.musicTitle.setText(music.getTitle());
        if (position == selectedPosition) {
            viewHolder.musicTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            viewHolder.musicTitle.setMarqueeRepeatLimit(-1);
            viewHolder.musicTitle.setSelected(true);
        } else {
            viewHolder.musicTitle.setEllipsize(null);
            viewHolder.musicTitle.setSelected(false);
        }

        viewHolder.musicDuration.setText(Music.formatDuration(music.getDuration()));

        return convertView;
    }
}

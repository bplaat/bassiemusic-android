package nl.plaatsoft.bassiemusic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MusicAdapter extends ArrayAdapter<Music> {
    private static class ViewHolder {
        TextView musicTitle;
        TextView musicDuration;
    }
    public MusicAdapter(Context context) {
       super(context, 0);
    }
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.music_item, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.musicTitle = (TextView)convertView.findViewById(R.id.music_title);
            viewHolder.musicDuration = (TextView)convertView.findViewById(R.id.music_duration);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }
        Music music = getItem(position);
        viewHolder.musicTitle.setText(music.getTitle());
        viewHolder.musicDuration.setText(Music.formatDuration(music.getDuration()));
        return convertView;
    }
}
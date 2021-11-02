package nl.plaatsoft.bassiemusic.components;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import nl.plaatsoft.bassiemusic.models.Music;
import nl.plaatsoft.bassiemusic.tasks.FetchCoverTask;
import nl.plaatsoft.bassiemusic.Utils;
import nl.plaatsoft.bassiemusic.R;

public class MusicAdapter extends ArrayAdapter<Music> implements SectionIndexer {
    private static class ViewHolder {
        public TextView musicPosition;
        public ImageView musicCover;
        public TextView musicTitle;
        public TextView musicArtists;
        public TextView musicAlbum;
        public TextView musicDuration;
    }

    private static class Section {
        public char character;
        public int position;
    }

    private List<Section> sections;
    private int selectedPosition = -1;

    public MusicAdapter(Context context) {
        super(context, 0);
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_music, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.musicPosition = (TextView)convertView.findViewById(R.id.music_position);
            viewHolder.musicCover = (ImageView)convertView.findViewById(R.id.music_cover);
            viewHolder.musicTitle = (TextView)convertView.findViewById(R.id.music_title);
            viewHolder.musicArtists = (TextView)convertView.findViewById(R.id.music_artists);
            viewHolder.musicAlbum = (TextView)convertView.findViewById(R.id.music_album);
            viewHolder.musicDuration = (TextView)convertView.findViewById(R.id.music_duration);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder)convertView.getTag();
        }

        if (position == selectedPosition) {
            convertView.setBackgroundResource(R.color.selected_background_color);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        Music music = getItem(position);

        viewHolder.musicPosition.setText(String.valueOf(music.getPosition()));

        FetchCoverTask.with(getContext()).fromMusic(music).fadeIn().into(viewHolder.musicCover).fetch();

        viewHolder.musicTitle.setText(music.getTitle());

        if (position == selectedPosition) {
            viewHolder.musicTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            viewHolder.musicTitle.setSelected(true);
        } else {
            viewHolder.musicTitle.setEllipsize(null);
            viewHolder.musicTitle.setSelected(false);
        }

        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        if (displayMetrics.widthPixels / displayMetrics.density < 600) {
            viewHolder.musicArtists.setText(String.join(", ", music.getArtists()) + " - " + music.getAlbum());
        } else {
            viewHolder.musicArtists.setText(String.join(", ", music.getArtists()));
            viewHolder.musicAlbum.setText(music.getAlbum());
        }

        if (position == selectedPosition) {
            viewHolder.musicArtists.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            viewHolder.musicArtists.setSelected(true);
        } else {
            viewHolder.musicArtists.setEllipsize(null);
            viewHolder.musicArtists.setSelected(false);
        }

        viewHolder.musicDuration.setText(Music.formatDuration(music.getDuration()));

        return convertView;
    }

    public void refreshSections() {
        sections = null;
    }

    @Override
    public Object[] getSections() {
        if (sections == null) {
            sections = new ArrayList<Section>();

            for (int position = 0; position < getCount(); position++) {
                Music music = getItem(position);
                char firstCharacter = Character.toUpperCase(music.getArtists().get(0).charAt(0));

                boolean isCharacterFound = false;
                for (Section section : sections) {
                    if (section.character == firstCharacter) {
                        isCharacterFound = true;
                        break;
                    }
                }

                if (!isCharacterFound) {
                    Section section = new Section();
                    section.character = firstCharacter;
                    section.position = position;
                    sections.add(section);
                }
            }
        }

        String[] sectionsArray = new String[sections.size()];
        for (int i = 0; i < sections.size(); i++) {
            sectionsArray[i] = String.valueOf(sections.get(i).character);
        }
        return sectionsArray;
    }

    @Override
    public int getPositionForSection(int section) {
        return sections.get(section).position;
    }

    @Override
    public int getSectionForPosition(int position) {
        Music music = getItem(position);
        char firstCharacter = Character.toUpperCase(music.getArtists().get(0).charAt(0));
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).character == firstCharacter) {
                return i;
            }
        }
        return 0;
    }
}

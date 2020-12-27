package nl.plaatsoft.bassiemusic;

import android.animation.AnimatorSet;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;
import java.util.List;
import java.util.ArrayList;

public class MusicAdapter extends ArrayAdapter<Music> implements SectionIndexer {
    private static class ViewHolder {
        public TextView musicTitle;
        public TextView musicDuration;
    }

    private static class Section {
        public char character;
        public int position;

        public Section(char character, int position) {
            this.character = character;
            this.position = position;
        }
    }

    private List<Section> sections = null;

    private int selectedPosition = -1;
    private boolean isSelectedPositionAnimated = false;
    private int oldSelectedPosition = -1;

    public MusicAdapter(Context context) {
        super(context, 0);
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        if (this.selectedPosition != selectedPosition) {
            oldSelectedPosition = this.selectedPosition;
            isSelectedPositionAnimated = false;
        } else {
            isSelectedPositionAnimated = true;
        }

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
            if (!isSelectedPositionAnimated) {
                AnimatorSet animation = (AnimatorSet)AnimatorInflater.loadAnimator(getContext(), R.animator.selected_music_in);
                animation.setTarget(convertView);
                animation.start();
                isSelectedPositionAnimated = true;
            } else {
                convertView.setBackgroundResource(R.color.selected_background_color);
            }
        }
        else if (position == oldSelectedPosition) {
            AnimatorSet animation = (AnimatorSet)AnimatorInflater.loadAnimator(getContext(), R.animator.selected_music_out);
            animation.setTarget(convertView);
            animation.start();
            oldSelectedPosition = -1;
        }
        else {
            convertView.setBackgroundColor(0);
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

    public Object[] getSections() {
        if (sections == null) {
            sections = new ArrayList<Section>();

            for (int position = 0; position < getCount(); position++) {
                Music music = getItem(position);
                char firstCharacter = Character.toUpperCase(music.getTitle().charAt(0));

                boolean isCharacterFound = false;
                for (Section section : sections) {
                    if (section.character == firstCharacter) {
                        isCharacterFound = true;
                        break;
                    }
                }

                if (!isCharacterFound) {
                    sections.add(new Section(firstCharacter, position));
                }
            }
        }

        String[] sectionsArray = new String[sections.size()];
        for (int i = 0; i < sections.size(); i++) {
            sectionsArray[i] = String.valueOf(sections.get(i).character);
        }
        return sectionsArray;
    }

    public int getPositionForSection(int section) {
        return sections.get(section).position;
    }

    public int getSectionForPosition(int position) {
        Music music = getItem(position);
        char firstCharacter = Character.toUpperCase(music.getTitle().charAt(0));
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).character == firstCharacter) {
                return i;
            }
        }
        return 0;
    }
}

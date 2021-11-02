package nl.plaatsoft.bassiemusic.activities;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.List;
import nl.plaatsoft.bassiemusic.components.MusicAdapter;
import nl.plaatsoft.bassiemusic.models.Music;
import nl.plaatsoft.bassiemusic.R;

public class SearchActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ((ImageButton)findViewById(R.id.search_back_button)).setOnClickListener((View view) -> {
            finish();
        });

        List<Music> music = Music.loadMusic(this);

        ScrollView startPage = (ScrollView)findViewById(R.id.search_start_page);

        ListView searchList = (ListView)findViewById(R.id.search_music_page);

        ScrollView emptyPage = (ScrollView)findViewById(R.id.search_empty_page);

        MusicAdapter searchAdapter = new MusicAdapter(this);
        searchList.setAdapter(searchAdapter);
        searchList.setOnItemClickListener((AdapterView<?> adapterView, View view, int position, long id) -> {
            Music musicItem = searchAdapter.getItem(position);
            Intent intent = getIntent();
            intent.putExtra("id", musicItem.getId());
            setResult(Activity.RESULT_OK, intent);
            finish();
        });

        EditText searchInput = (EditText)findViewById(R.id.search_input);

        searchInput.setOnEditorActionListener((TextView view, int actionId, KeyEvent event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            }
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String searchQuery = charSequence.toString().toLowerCase();

                searchAdapter.clear();

                if (searchQuery.length() >= 1) {
                    for (Music musicItem : music) {
                        if (
                            String.join(", ", musicItem.getArtists()).toLowerCase().contains(searchQuery) ||
                            musicItem.getAlbum().toLowerCase().contains(searchQuery) ||
                            musicItem.getTitle().toLowerCase().contains(searchQuery)
                        ) {
                            searchAdapter.add(musicItem);
                        }
                    }

                    if (searchAdapter.getCount() > 0) {
                        startPage.setVisibility(View.GONE);
                        searchList.setVisibility(View.VISIBLE);
                        emptyPage.setVisibility(View.GONE);
                    } else {
                        startPage.setVisibility(View.GONE);
                        searchList.setVisibility(View.GONE);
                        emptyPage.setVisibility(View.VISIBLE);
                    }
                } else {
                    startPage.setVisibility(View.VISIBLE);
                    searchList.setVisibility(View.GONE);
                    emptyPage.setVisibility(View.GONE);
                }

                searchList.setSelectionAfterHeaderView();
            }

            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            public void afterTextChanged(Editable editable) {}
        });


        View.OnClickListener clearSearchInput = (View view) -> {
            searchInput.setText("");
        };
        ((ImageButton)findViewById(R.id.search_clear_button)).setOnClickListener(clearSearchInput);
        ((Button)findViewById(R.id.search_empty_hero_button)).setOnClickListener(clearSearchInput);
    }
}

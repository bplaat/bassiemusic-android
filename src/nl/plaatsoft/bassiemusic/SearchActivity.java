package nl.plaatsoft.bassiemusic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

public class SearchActivity extends BaseActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ((ImageView)findViewById(R.id.search_back_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                finish();
            }
        });

        ArrayList<Music> music = Music.loadMusic(this);

        LinearLayout startPage = (LinearLayout)findViewById(R.id.search_start_page);

        ListView searchList = (ListView)findViewById(R.id.search_list);

        LinearLayout emptyPage = (LinearLayout)findViewById(R.id.search_empty_page);

        MusicAdapter searchAdapter = new MusicAdapter(this);
        searchList.setAdapter(searchAdapter);
        searchList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Music musicItem = searchAdapter.getItem(position);
                Intent intent = getIntent();
                intent.putExtra("id", musicItem.getId());
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        EditText searchInput = (EditText)findViewById(R.id.search_input);
        searchInput.requestFocus();

        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    return true;
                }
                return false;
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String searchQuery = charSequence.toString().toLowerCase();

                searchAdapter.clear();

                if (searchQuery.length() >= 1) {
                    for (Music musicItem : music) {
                        if (musicItem.getTitle().toLowerCase().contains(searchQuery)) {
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

        ((ImageView)findViewById(R.id.search_clear_button)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                searchInput.setText("");
            }
        });
    }
}

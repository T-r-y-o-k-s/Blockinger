/*
 * Copyright 2013 Simon Willeke
 * contact: hamstercount@hotmail.com
 */

/*
    This file is part of Blockinger.

    Blockinger is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Blockinger is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Blockinger.  If not, see <http://www.gnu.org/licenses/>.

    Diese Datei ist Teil von Blockinger.

    Blockinger ist Freie Software: Sie können es unter den Bedingungen
    der GNU General Public License, wie von der Free Software Foundation,
    Version 3 der Lizenz oder (nach Ihrer Option) jeder späteren
    veröffentlichten Version, weiterverbreiten und/oder modifizieren.

    Blockinger wird in der Hoffnung, dass es nützlich sein wird, aber
    OHNE JEDE GEWÄHELEISTUNG, bereitgestellt; sogar ohne die implizite
    Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK.
    Siehe die GNU General Public License für weitere Details.

    Sie sollten eine Kopie der GNU General Public License zusammen mit diesem
    Programm erhalten haben. Wenn nicht, siehe <http://www.gnu.org/licenses/>.
 */

package org.blockinger.game.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import org.blockinger.game.R;
import org.blockinger.game.components.GameState;
import org.blockinger.game.components.Sound;
import org.blockinger.game.db.ScoreDataSource;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.blockinger.game.db.HighscoreOpenHelper.*;

public class MainActivity extends ListActivity {

    public static final int SCORE_REQUEST = 0x0;

    /**
     * This key is used to access the player name, which is returned as an Intent from the gameactivity upon completion (gameover).
     * The Package Prefix is mandatory for Intent data
     */
    public static final String PLAYERNAME_KEY = "org.blockinger.game.activities.playername";

    /**
     * This key is used to access the player name, which is returned as an Intent from the gameactivity upon completion (gameover).
     * The Package Prefix is mandatory for Intent data
     */
    public static final String SCORE_KEY = "org.blockinger.game.activities.score";

    public ScoreDataSource datasource;
    private SimpleCursorAdapter adapter;
    private AlertDialog.Builder startLevelDialog;
    private AlertDialog.Builder donateDialog;
    private int startLevel;
    private View dialogView;
    private SeekBar leveldialogBar;
    private TextView leveldialogtext;
    private Sound sound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setActionBar(findViewById(R.id.toolbar));
        }
        PreferenceManager.setDefaultValues(this, R.xml.simple_preferences, true);
        PreferenceManager.setDefaultValues(this, R.xml.advanced_preferences, true);

        /* Create Music */
        sound = new Sound(this);
        sound.startMusic(Sound.MENU_MUSIC, 0);

        /* Database Management */
        datasource = new ScoreDataSource(this);
        datasource.open();
        Cursor cursor = datasource.getCursor();
        // Use the SimpleCursorAdapter to show the
        // elements in a ListView
        adapter = new SimpleCursorAdapter(
                this,
                R.layout.blockinger_list_item,
                cursor,
                new String[]{COLUMN_SCORE, COLUMN_PLAYERNAME, COLUMN_DATE},
                new int[]{R.id.list_item_score, R.id.list_item_player_name, R.id.list_item_date},
                SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            private final SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm", Locale.US);

            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.list_item_date) {
                    long dateTime = cursor.getLong(columnIndex);
                    Date date = new Date();
                    date.setTime(dateTime);
                    Calendar yearsAgoCalendar = Calendar.getInstance();
                    yearsAgoCalendar.set(Calendar.YEAR, 2000);

                    String dateString;
                    if (date.before(yearsAgoCalendar.getTime())) dateString = getString(R.string.main_years_ago);
                    else dateString = sdf.format(date);

                    ((TextView) view).setText(dateString);
                    return true;
                }
                return false;
            }
        });
        setListAdapter(adapter);

        /* Create Startlevel Dialog */
        startLevel = 0;
        startLevelDialog = new AlertDialog.Builder(this);
        startLevelDialog.setTitle(R.string.startLevelDialogTitle);
        startLevelDialog.setCancelable(false);
        startLevelDialog.setNegativeButton(R.string.startLevelDialogCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        startLevelDialog.setPositiveButton(R.string.startLevelDialogStart, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.this.start();
            }
        });

        /* Create Donate Dialog */
        donateDialog = new AlertDialog.Builder(this);
        donateDialog.setTitle(R.string.pref_donate_title);
        donateDialog.setMessage(R.string.pref_donate_summary);
        donateDialog.setNegativeButton(R.string.startLevelDialogCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        donateDialog.setPositiveButton(R.string.donate_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = getResources().getString(R.string.donation_url);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                Intent intent1 = new Intent(this, AboutActivity.class);
                startActivity(intent1);
                return true;
            case R.id.action_donate:
                donateDialog.show();
                return true;
            case R.id.action_help:
                Intent intent2 = new Intent(this, HelpActivity.class);
                startActivity(intent2);
                return true;
            case R.id.action_exit:
                GameState.destroy();
                MainActivity.this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void start() {
        Intent intent = new Intent(this, GameActivity.class);
        Bundle b = new Bundle();
        b.putInt("mode", GameActivity.NEW_GAME); //Your id
        b.putInt("level", startLevel); //Your id
        b.putString("playername", ((TextView) findViewById(R.id.nicknameEditView)).getText().toString()); //Your id
        intent.putExtras(b); //Put your id to your next Intent
        startActivityForResult(intent, SCORE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != SCORE_REQUEST)
            return;
        if (resultCode != RESULT_OK)
            return;

        String playerName = data.getStringExtra(PLAYERNAME_KEY);
        long score = data.getLongExtra(SCORE_KEY, 0);

        datasource.open();
        datasource.createScore(score, playerName, new Date());
    }


    public void onClickStart(View view) {
        dialogView = getLayoutInflater().inflate(R.layout.seek_bar_dialog, null);
        leveldialogtext = dialogView.findViewById(R.id.leveldialogleveldisplay);
        leveldialogBar = dialogView.findViewById(R.id.levelseekbar);
        leveldialogBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                leveldialogtext.setText("" + arg1);
                startLevel = arg1;
            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
            }

        });
        leveldialogBar.setProgress(startLevel);
        leveldialogtext.setText("" + startLevel);
        startLevelDialog.setView(dialogView);
        startLevelDialog.show();
    }

    public void onClickResume(View view) {
        Intent intent = new Intent(this, GameActivity.class);
        Bundle b = new Bundle();
        b.putInt("mode", GameActivity.RESUME_GAME); //Your id
        b.putString("playername", ((TextView) findViewById(R.id.nicknameEditView)).getText().toString()); //Your id
        intent.putExtras(b); //Put your id to your next Intent
        startActivityForResult(intent, SCORE_REQUEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sound.pause();
        sound.setInactive(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sound.pause();
        sound.setInactive(true);
        datasource.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sound.release();
        sound = null;
        datasource.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sound.setInactive(false);
        sound.resume();
        datasource.open();
        Cursor cursor = datasource.getCursor();
        adapter.changeCursor(cursor);

        if (!GameState.isFinished()) {
            findViewById(R.id.resumeButton).setEnabled(true);
            ((Button) findViewById(R.id.resumeButton)).setTextColor(getResources().getColor(R.color.square_error));
        } else {
            findViewById(R.id.resumeButton).setEnabled(false);
            ((Button) findViewById(R.id.resumeButton)).setTextColor(getResources().getColor(R.color.holo_grey));
        }
    }

}

package com.example.distinctionapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.material.snackbar.Snackbar;


public class MainActivity extends AppCompatActivity {

    /** chapter番号 */
    private int chapter = 1;

    /**
     * section <br>
     * ["academic", "phrase", "sentence", "snapshot", "word"]
     */
    private String section = "academic";

    /** chapter sectionの何個目の音源か */
    private int index = 0;

    /** mp3再生関連のインスタンス */
    private MediaPlayer mediaPlayer = new MediaPlayer();

    /** 全mp3ファイル名情報 */
    private JSONObject mp3FilenameJson;

    /**
     * chapter, sectionをしていた際のmp3ファイル名情報 <br>
     * "data"キー : mp3ファイル名の一覧 (配列) <br>
     * "len"キー : mp3ファイルの数
     */
    private JSONObject jsonChapterSection;

    /** 再生するmp3ファイル */
    private String mp3File;

    /** jsonChapterSectionのdataの数 */
    private int lenChapterSection;

    /** mp3音源が流れているかどうか */
    private boolean isMediaPlay;

    /** speedのSpinner */
    private Spinner spinnerSpeed;

    /** chapterのSpinner */
    private Spinner spinnerChapter;

    /** sectionのSpinner */
    private Spinner spinnerSection;

    /** mp3の再生位置 */
    private int mp3CurrentPosition = 0;


    /**
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // spinnerを使用し、ListBox作成
        spinnerSpeed = (Spinner) findViewById(R.id.speed_spinner);
        spinnerChapter = (Spinner) findViewById(R.id.chapter_spinner);
        spinnerSection = (Spinner) findViewById(R.id.section_spinner);
        createListBox(spinnerSpeed, R.array.speed_state);
        createListBox(spinnerChapter, R.array.chapter_state);
        createListBox(spinnerSection, R.array.section_state);

        // 音量調整
        //adjustMusicVolume();

        // mp3のファイル名取得
        mp3FilenameJson = getFilenameJson();
        jsonChapterSection = getJsonChapterSection();
        mp3FilenameJson = null;
        mp3File = getMp3File();
        lenChapterSection = getLenChapterSection();

        // mediaPlayerにmp3ファイルをセット
        setMediaPlayerMp3();

        // mediaPlayer設定
        setMediaPlayerOption();

        // playボタンを押した時の処理
        findViewById(R.id.button_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickPlay(v);
            }
        });

        // stopボタンを押した時の処理
        findViewById(R.id.button_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickStop(v);
            }
        });

        // nextボタンを押した時の処理
        findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickNext(v);
            }
        });

        // backボタンを押した時の処理
        findViewById(R.id.button_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickBack(v);
            }
        });

        // 5s backボタンを押した時の処理
        findViewById(R.id.button_5sback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick5sBack(v);
            }
        });

        // 10s backボタンを押した時の処理
        findViewById(R.id.button_10sback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick10sBack(v);
            }
        });

        // setボタンを押した時の処理
        findViewById(R.id.button_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickSet(v);
            }
        });

        // speedを変更した時の処理
        spinnerSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                changePlaySpeed();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }


    /**
     * リストボックス作成関数
     * @param spinner 作成したいリストボックスのspinner
     * @param arrayId styleに書かれたのリストボックスのitem情報
     */
    private void createListBox(Spinner spinner, int arrayId) {
        // styleの情報を読み、選択肢の配列作成
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, arrayId, android.R.layout.simple_spinner_item);

        // spinnerに適用
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }


    /**
     * playボタンを押したときの処理 <br>
     * 音源の再生
     * @param v
     */
    private void onClickPlay(View v) {
        if (!(isMediaPlay)) {
            // mp3音源が流れていないとき、音源を再生
            try{
                isMediaPlay = true;
                mediaPlayer.prepare();
                changePlaySpeed();
                mediaPlayer.seekTo(mp3CurrentPosition);
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // mp3音源がすでに流れているとき、なにも処理をしない
        }
    }


    /**
     * stopボタンを押したときの処理 <br>
     * 音源の停止
     * @param v
     */
    private void onClickStop(View v) {
        if (isMediaPlay) {
            // mp3音源が流れているとき、音源を停止
            isMediaPlay = false;
            mp3CurrentPosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.stop();

        } else {
            // mp3音源が流れていないとき、何も処理をしない
        }
    }


    /**
     * nextボタンを押したときの処理 <br>
     * 次の音源を再生
     * @param v
     */
    private void onClickNext(View v) {
        if (index == lenChapterSection - 1) {
            // 次の音源ファイルがない場合、それを通知
            Snackbar.make(v, "finish", Snackbar.LENGTH_SHORT).show();

        } else {
            // 次の音源ファイルがある場合
            // 音源を停止
            onClickStop(v);

            // mediaPlayerをreset
            mediaPlayer.reset();

            // mediaPlayerの初期設定
            setMediaPlayerOption();

            // mp3ファイルを取得
            index += 1;
            mp3File = getMp3File();

            // mediaPlayerに音源をセット
            setMediaPlayerMp3();
            mp3CurrentPosition = 0;

            // 音源再生
            onClickPlay(v);
        }
    }


    /**
     * backボタンを押したときの処理 <br>
     * 前の音源を再生
     * @param v
     */
    private void onClickBack(View v) {
        // 音源を停止
        onClickStop(v);

        // mediaPlayerをreset
        mediaPlayer.reset();

        // mediaPlayerの初期設定
        setMediaPlayerOption();

        // 前のmp3File取得
        index = Math.max(index - 1, 0);
        mp3File = getMp3File();

        // mediaPlayerに音源をセット
        setMediaPlayerMp3();
        mp3CurrentPosition = 0;

        // 音源再生
        onClickPlay(v);
    }


    /**
     * 5s backボタンを押したときの処理 <br>
     * 再生位置を5秒巻き戻す
     * @param v
     */
    private void onClick5sBack(View v) {
        if (isMediaPlay) {
            nsBack(5);
        }
    }


    /**
     * 10s backボタンを押したときの処理 <br>
     * 再生位置を5秒巻き戻す
     * @param v
     */
    private void onClick10sBack(View v) {
        if (isMediaPlay) {
            nsBack(10);
        }
    }


    /**
     * setボタンを押したときの処理 <br>
     * chapter, section の設定
     * @param v
     */
    private void onClickSet(View v) {
        // 音源を停止
        onClickStop(v);

        // mediaPlayerをreset
        mediaPlayer.reset();

        // mediaPlayerの初期設定
        setMediaPlayerOption();

        // リストボックスの内容を取得
        chapter = Integer.parseInt((String) spinnerChapter.getSelectedItem());
        section = (String) spinnerSection.getSelectedItem();

        // indexは0に
        index = 0;

        // mp3のファイル名取得
        mp3FilenameJson = getFilenameJson();
        jsonChapterSection = getJsonChapterSection();
        mp3FilenameJson = null;
        mp3File = getMp3File();
        lenChapterSection = getLenChapterSection();

        // mediaPlayerに音源をセット
        setMediaPlayerMp3();
        mp3CurrentPosition = 0;

    }


    /**
     * mediaPlayerにmp3ファイルをセットする関数
     */
    private void setMediaPlayerMp3() {
        try{
            AssetFileDescriptor descriptor = getAssets().openFd(mp3File);
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * mediaPlayerのオプションをセット
     */
    private void setMediaPlayerOption() {
        // ループ再生on
        mediaPlayer.setLooping(true);

        // 左右の音の大きさの割合を一緒に
        mediaPlayer.setVolume(1f, 1f);
    }

    /**
     * 音量を調整する関数
     */
    private void adjustMusicVolume() {
        // 最大音量値を取得
        AudioManager audioManager = (AudioManager)getSystemService(this.AUDIO_SERVICE);
        int vol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        // 音量を設定
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) (vol / 2), 0);
    }


    /**
     * 音源の再生位置をn[s]バックする関数
     * @param n 何秒再生位置をバックするか
     */
    private void nsBack(int n) {
        // 再生位置が0sより小さくならないようにする
        mp3CurrentPosition = Math.max(0, mediaPlayer.getCurrentPosition() - (n * 1000));
        mediaPlayer.seekTo(mp3CurrentPosition);
    }


    /**
     * mp3ファイル名が書かれたjsonファイルを取得 <br>
     * 以下のサイト参考 <br>
     * https://mjeeeey.hatenablog.com/entry/2020/07/13/215929
     * http://ykonp.com/post-2391/
     * @return jsonObj mp3ファイルの情報を格納
     */
    private JSONObject getFilenameJson() {
        // 必要変数定義
        JSONObject jsonObj = null;
        InputStream inputStream = null;
        BufferedReader br = null;

        try {
            // ファイル読み込み
            inputStream = this.getAssets().open("mp3FilenameData.json");
            br = new BufferedReader(new InputStreamReader(inputStream));

            // テキストを取得
            String strLine;
            StringBuilder sbSentence = new StringBuilder();
            while ((strLine = br.readLine()) != null) {
                sbSentence.append(strLine);
            }

            // JSONオブジェクトのインスタンス作成
            jsonObj = new JSONObject(sbSentence.toString());

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return jsonObj;
    }


    /**
     * chapter, sectionをしていした際のファイル一覧を返す関数
     * @return chapter, sectionに応じたmp3ファイル名の一覧 <br>
     *  "data"キー : mp3ファイル名の一覧 (配列) <br>
     *  "len"キー : mp3ファイルの数
     */
    private JSONObject getJsonChapterSection() {
        // 必要変数定義
        JSONObject result = null;

        try {
            // ファイル名取得
            JSONObject item = mp3FilenameJson.getJSONObject(String.valueOf(chapter));
            result = item.getJSONObject(section);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }


    /**
     * chapter, section, indexを指定したときのmp3ファイル名を返す関数
     * @return chapter, section, indexを指定した時のファイル名のURI
     */
    private String getMp3File() {
        // chapter 0埋め
        String chapter0Padding = null;
        if (1 <= chapter && chapter <= 9) {
            chapter0Padding = "0" + String.valueOf(chapter);
        } else {
            chapter0Padding = String.valueOf(chapter);
        }

        // mp3ファイル名
        String result = null;
        try {
            JSONArray jsonArray = jsonChapterSection.getJSONArray("data");
            result = jsonArray.getString(index);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "mp3file/" + chapter0Padding + "/" + section + "/" + result;
    }


    /**
     * jsonChapterSectionのlenキーの取得
     * @return chapter, sectionを指定したとき、何個データがあるか
     */
    private int getLenChapterSection() {
        int result = -1;
        try {
            result = Integer.parseInt(jsonChapterSection.getString("len"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }


    /**
     * 音源の再生速度を変化させる関数
     */
    private void changePlaySpeed() {
        if (isMediaPlay) {
            // リストボックスの内容を取得
            float playSpeed = Float.parseFloat((String) spinnerSpeed.getSelectedItem());

            // 再生速度変更
            mediaPlayer.setPlaybackParams(new PlaybackParams().setSpeed(playSpeed));
        }
    }


    /**
     * 参考URL
     * ----------------------------------------
     * MediaPlayerの状態遷移図
     * https://developer.android.com/reference/android/media/MediaPlayer.html
     */
}
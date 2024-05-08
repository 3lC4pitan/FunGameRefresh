package com.hitomi.fungamerefreshdemo;

import android.os.SystemClock;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.hitomi.refresh.view.FunGameRefreshView;

public class ListViewActivity extends BaseActivity {

    private ListView listView;

    @Override
    public void setContentView() {
        setContentView(R.layout.activity_list_view);
    }

    @Override
    public void initView() {
        refreshView = (FunGameRefreshView) findViewById(R.id.refresh_fun_game);
        refreshView.setLoadingText("Play a game to relieve boredom");
        refreshView.setGameOverText("game over");
        refreshView.setLoadingFinishedText("Loading completed");
        refreshView.setTopMaskText("Pull down to refresh");
        refreshView.setBottomMaskText("Swipe up and down to control the game");

        listView = (ListView) findViewById(R.id.list_view);
    }

    @Override
    public void setViewListener() {
        refreshView.setOnRefreshListener(new FunGameRefreshView.FunGameRefreshListener() {
            @Override
            public void onPullRefreshing() {
                SystemClock.sleep(2000);
            }

            @Override
            public void onRefreshComplete() {
                updateDataList();
                baseAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void processLogic() {
        baseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, createDate());
        listView.setAdapter(baseAdapter);
    }

}

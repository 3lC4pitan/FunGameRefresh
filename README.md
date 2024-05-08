# FunGameRefreshView

[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-HitBlockRefresh-brightgreen.svg?style=flat)](http://android-arsenal.com/details/3/3253)

An interesting and fun pull-to-refresh library. Do you remember playing games on the black and white handheld game console when you were a child?

## Preview

<img src="preview/HitBlock.gif" width="350px" />
<img src="preview/BattleCity.gif"  width="350px" />

## FunGame

### Update content 
- **2016-07-28**
  - When the background thread has not finished executing, releasing the finger will cause the pull-to-refresh control to roll back to the height of the game area, and the user can still continue to play the game.
  - Reorganized the logic for displaying characters in the center of the game area, no more flickering characters.
- **2016-08-01**
  - Fixed the problem of animation execution error and inability to operate the game when the background thread execution time is too short.
- **2016-08-17**
  - Expanded custom attributes to add custom text in the pull-to-refresh header.
- **2016-12-02**
  - FunGameRefreshView supports GridView, ListView, RecycleView, and other controls.
  - Only one child control can be placed in FunGameRefreshView.
  - Fixed the problem of an error occurring when the game over prompt language is not set.
  - Fixed the problem of not being able to completely slide back the manually controlled pull-to-refresh header.

Currently supports two games: Hit Block and Battle City.
The rules for hitting blocks are simple, if you fail to catch the ball, it's Game Over.
The rules for Battle City are: Game Over if you miss more than ten enemy tanks or collide with an enemy tank. See if you can last three minutes. Hehe~;

## Usage

In the layout file:
```xml
<com.hitomi.refresh.view.FunGameRefreshView
    android:id="@+id/refresh_hit_block"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:game_type="hit_block">
            
    <ListView
        android:id="@+id/list_view"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        android:scrollbars="none">
    </ListView>
    
</com.hitomi.refresh.view.FunGameRefreshView>

    In the Activity:

        refreshView = (FunGameRefreshView) findViewById(R.id.refresh_fun_game);
        listView = (ListView) findViewById(R.id.list_view);

        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, createDate());
        listView.setAdapter(arrayAdapter);
            
        refreshView.setOnRefreshListener(new FunGameRefreshView.FunGameRefreshListener() {
            @Override
            public void onPullRefreshing() {
                // Simulate background time-consuming tasks
                SystemClock.sleep(2000);
            }

            @Override
            public void onRefreshComplete() {
                updateDataList();
                arrayAdapter.notifyDataSetChanged();
            }
        });

        
For specific sample code, please refer to the code in the [fungamerefreshdemo](https://github.com/3lC4pitan/FunGameRefresh/tree/master/fungamerefreshdemo/src/main/java/com/hitomi/fungamerefreshdemo) package.

# Attributes

    It supports switching games in the pull-to-refresh header:

        <attr name="game_type" format="enum">
            <enum name="hit_block" value="0" />
            <enum name="battle_city" value="1" />
        </attr>

    It supports customizing the colors of various parts in the pull-to-refresh header:

        <attr name="left_model_color" format="color" />
        <attr name="middle_model_color" format="color" />
        <attr name="right_model_color" format="color" />

    It supports customizing text in the pull-to-refresh header:

        <attr name="mask_top_text" format="string" />
        <attr name="mask_bottom_text" format="string" />
        <attr name="text_loading" format="string" />
        <attr name="text_loading_finished" format="string" />
        <attr name="text_game_over" format="string" />

        <attr name="top_text_size" format="integer" />
        <attr name="bottom_text_size" format="integer" />

    It supports customizing the number of columns and the speed of the ball in the HitBlock game:      

        <attr name="block_horizontal_num" format="integer" />
        <attr name="ball_speed" format="integer">
            <enum name="low" value="3" />
            <enum name="medium" value="6" />
            <enum name="fast" value="9" />
        </attr>


#Thanks

The UI design is from: https://github.com/dasdom/BreakOutToRefresh

#Licence

MIT 




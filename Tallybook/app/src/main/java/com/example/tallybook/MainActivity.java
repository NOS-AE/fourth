package com.example.tallybook;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.LitePal;
import org.litepal.LitePalApplication;
import org.litepal.LitePalBase;
import org.litepal.LitePalDB;
import org.litepal.crud.LitePalSupport;
import org.litepal.parser.LitePalAttr;
import org.litepal.parser.LitePalContentHandler;
import org.litepal.tablemanager.Connector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<Item> itemList;

    private ItemAdapter adapter;

    private int mCheckCount = 0;

    private ItemTouchHelperCallback callback;

    private AppBarLayout.LayoutParams params;

    private Toolbar toolbar;

    private CheckBox checkAllBox;

    private RecyclerView recyclerView;

    private FloatingActionButton fab;

    private boolean checkMode = false;

    private int editIndex;

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //创建数据库
        db = LitePal.getDatabase();
        //初始化成员控件
        initWidget();
        //从数据库取出数据并设置数据更新视图
        initItemForTest();
        //初始化Adapter
        initAdapter();


        //初始化Item分类列表
        Item.initCategory();
        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayShowTitleEnabled(false);

        //初始化显示
        showHideHint(itemList.isEmpty());
    }

    private void initAdapter(){
        //设置recyclerView布局
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ItemAdapter(itemList,this);
        //adapter的item注册Listener
        adapter.setOnItemClickListener(new ItemAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view) {
                //编辑收支
                Intent intent = new Intent(MainActivity.this,SetDataActivity.class);
                intent.putExtra("isEdit",true);
                intent.putExtra("Item",itemList.get((Integer) view.getTag()));
                editIndex = (Integer)view.getTag();
                startActivityForResult(intent,10);//10编辑
            }

            @Override
            public void onCheck(View view) {
                //选择收支
                checkBoxOnClick(view);
            }
        });
        adapter.setOnStateChecker(new ItemAdapter.StateChecker() {
            @Override
            public void onCheck(int i) {
                //从数据库删除数据
                LitePal.delete(Item.class,i);
                showHideHint(itemList.isEmpty());
            }
        });
        //recyclerView绑定adapter
        recyclerView.setAdapter(adapter);
        //为recyclerView绑定ItemTouchHelper，实现拖动删除和复原
        callback = new ItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    private void initItemForTest(){
        itemList = LitePal.order("id desc").find(Item.class);
    }

    private void initWidget(){
        fab = findViewById(R.id.fab);
        toolbar = findViewById(R.id.toolbar);
        params = (AppBarLayout.LayoutParams)toolbar.getLayoutParams();
        checkAllBox = findViewById(R.id.check_all);
        recyclerView = findViewById(R.id.recycler_view);

    }

    //多选模式下（onClick）
    public void checkBoxOnClick(View view) {
        View checkedView = view.findViewById(R.id.checked);
        Item item = itemList.get((Integer) view.findViewById(R.id.unchecked).getTag());

        //是否刚进入多选模式
        if(!checkMode){
            enterCheckedState();
        }

        if(item.isChecked()){
            item.setChecked(false);
            checkedView.setVisibility(View.GONE);
            mCheckCount--;
        }
        else{
            item.setChecked(true);
            checkedView.setVisibility(View.VISIBLE);
            mCheckCount++;
        }

        //是否全部选中/
        if(mCheckCount == itemList.size()){
            checkAllBox.setChecked(true);
        }
        else if(checkAllBox.isChecked()){
            checkAllBox.setChecked(false);
        }
        //全不选状态下，fab禁止使用
        if(mCheckCount == 0){
            fab.setEnabled(false);
            fab.bringToFront();//setEnable(false)后被置下层，因此使用此方法
        }
        //fab恢复可使用状态
        else if(!fab.isEnabled()){
            fab.setEnabled(true);
        }
    }

    //点击全选的CheckBox（onClick）
    public void checkAllBoxOnClick(View view) {
        if (((CheckBox)view).isChecked()) {
            for (Item item : itemList) {
                item.setChecked(true);
            }
            mCheckCount = itemList.size();
            fab.setEnabled(true);
        }
        else{
            for (Item item : itemList) {
                item.setChecked(false);
            }
            mCheckCount = 0;
            fab.setEnabled(false);
            fab.bringToFront();
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        //退出多选模式
        if(checkMode){
            exitCheckedState(false);
        }
        else{
            //保存数据
            for (Item item :itemList) {
                item.save();
            }
            super.onBackPressed();
        }

    }

    //点击fab（onClick）
    public void onFabClick(View view) {
        //多选模式下为删除
        if (checkMode) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("确认");
            builder.setMessage("是否确认删除这" + mCheckCount + "项");
            builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    exitCheckedState(true);
                }
            })
                    .setNegativeButton("否", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //do nothing
                        }
                    }).show();

        } else {//非多选则为新建item
            Intent intent = new Intent(this,SetDataActivity.class);
            intent.putExtra("isEdit",false);
            startActivityForResult(intent,20);//20新建
        }
    }

    //数据是否已被删除完，是则更新主页面视图
    public void showHideHint(boolean isShow){
        if (isShow) {
            findViewById(R.id.no_data_tv).setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        else{
            findViewById(R.id.no_data_tv).setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    //退出多选模式
    //param:是否是从多选删除后的退出多选
        //true:更新视图后，做删除处理
        //false:更新视图
    private void exitCheckedState(boolean isFromFabDelete){
        findViewById(R.id.check_all).setVisibility(View.INVISIBLE);
        adapter.setChecked(false);
        checkMode = false;
        //改变fab样式为添加，开启动画，设为可用
        FabScrollBehavior.setAnimatorEnable(true);
        fab.setImageResource(R.drawable.add5);
        fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.colorTheme)));
        fab.setEnabled(true);
            //将动画放置ui线程或setEnable后避免setEnable使fab动画瞬间被完成
        FabScrollBehavior.show(fab);

        //开启item滑动
        callback.setSwipeEnable(true);
        //开启toolbar动画
        params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP |
                AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS |
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        toolbar.setLayoutParams(params);


        //更改界面每个item的选择显示
            //fabDelete下，更新recyclerView视图交给多选删除处理
        if(!isFromFabDelete)
            adapter.notifyDataSetChanged();
        //隐藏全部toolbar中选择图标
        CheckBox checkBox = findViewById(R.id.check_all);
        checkBox.setVisibility(View.INVISIBLE);
        checkBox.setChecked(false);

        //多选删除处理
        if (isFromFabDelete){
            for(int i = 0;i < itemList.size();i++){
                if(itemList.get(i).isChecked()) {//被选中的
                    itemList.remove(i);
                    i--;//以防漏掉被删除后的一项未处理
                    //通知adapter更新数据和界面
                    adapter.notifyItemRemoved(i+1);
                    adapter.notifyItemRangeChanged(0,itemList.size());
                }
            }
            showHideHint(itemList.isEmpty());
        }
        for(Item item:itemList){
            item.setChecked(false);
        }
        mCheckCount = 0;
    }

    //进入多选模式
    private void enterCheckedState(){
        checkAllBox.setVisibility(View.VISIBLE);
        adapter.setChecked(true);
        checkMode = true;
        //fab更改样式为删除，关闭动画
        fab.setImageResource(R.drawable.delete5);
        fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,R.color.red)));
        FabScrollBehavior.show(fab);
        FabScrollBehavior.setAnimatorEnable(false);
        // 禁止拖动项目删除
        callback.setSwipeEnable(false);
        //禁止toolbar隐藏
        params.setScrollFlags(0);
        toolbar.setLayoutParams(params);
    }

    //接收SetDataActivity返回的数据
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null && data.getBooleanExtra("isOK",false)){
            if(requestCode == 10){
                //编辑完成
                itemList.remove(editIndex);
                itemList.add(editIndex,(Item) data.getSerializableExtra("Item"));
                adapter.notifyItemChanged(editIndex);
            }
            else if(requestCode == 20){
                //新建完成
                itemList.add(0,(Item)data.getSerializableExtra("Item"));
                showHideHint(false);
                adapter.notifyDataSetChanged();
            }
        }
    }
}

package com.example.tallybook;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.bigkoo.pickerview.builder.TimePickerBuilder;
import com.bigkoo.pickerview.listener.OnTimeSelectListener;
import com.bigkoo.pickerview.view.TimePickerView;
import org.litepal.LitePal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

//最可改进：将源列表与其他表（目前只有sortList工作）封装到自定义List类中
//改进：风格
public class MainActivity extends AppCompatActivity {

    private List<Item> itemList;//源列表

    private List<Item> sortList = new ArrayList<>();//adapter全程使用sortList

    private double[] yAll = {0.0,0.0},
        mAll = {0.0,0.0},
        dAll = {0.0,0.0};

    private ItemAdapter adapter;

    private int mCheckCount = 0;

    private ItemTouchHelperCallback callback;

    private AppBarLayout.LayoutParams params;

    private Toolbar toolbar;

    private TextView labelTV, yearTV, monthTV, dayTV;

    private CheckBox checkAllBox;

    private RecyclerView recyclerView;

    private FloatingActionButton fab;

    private TimePickerView timePickerView;

    private Date pickerDate;

    private Spinner IOSpinner;

    private Menu menu;//多选时需隐藏目录

    private Dialog helpDialog;//展示功能及使用

    private List<String> spinnerList = new ArrayList<>();

    private boolean checkMode = false;

    private int editIndex;

    private int sortDateId = 0;//按时间分类：全部0，年0，月0，日0

    private int sortIOId = 0;//按收入支出分类：全部0，支出1，收入2

    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //创建数据库
        db = LitePal.getDatabase();
        //初始化用于optionPicker的列表
        initPicker();
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
        showHideHint(adapter.getItemCount());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //按时间分类
        switch (item.getItemId()){
            case R.id.all:
                sortDateId = 0;
                break;
            case R.id.year:
                sortDateId = 1;
                Toast.makeText(this,"选择所需的年即可",Toast.LENGTH_SHORT).show();
                break;
            case R.id.month:
                sortDateId = 2;
                Toast.makeText(this, "选择所需的年月即可", Toast.LENGTH_SHORT).show();
                break;
            case R.id.day:
                sortDateId = 3;
                Toast.makeText(this, "选择所需的年月日即可", Toast.LENGTH_SHORT).show();

                break;
                default:
        }
        if(sortDateId != 0)
            timePickerView.show();
        else
            sortById(sortIOId,sortDateId,pickerDate);
        return true;
    }

    private void initPicker(){
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR,-2);
        minDate.set(Calendar.MONTH,0);
        minDate.set(Calendar.DAY_OF_MONTH,1);
        timePickerView = new TimePickerBuilder(this, new OnTimeSelectListener() {
            @Override
            public void onTimeSelect(Date date, View v) {
                pickerDate = date;
                sortById(sortIOId,sortDateId,date);
            }
        })
            .setType(new boolean[]{true,true,true,false,false,false})
            .setRangDate(minDate,Calendar.getInstance())
            .setDate(Calendar.getInstance())
            .setSubmitColor(getColor(R.color.colorPrimaryDark))
            .setCancelColor(getColor(R.color.colorPrimaryDark))
            .setTitleSize(18)
            .setTitleText("筛选").build();
    }

    private void initAdapter(){
        //设置recyclerView布局
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ItemAdapter(sortList,this);
        //adapter的item注册Listener
        adapter.setOnItemClickListener(new ItemAdapter.OnItemClickListener() {
            @Override
            public void onClick(View view) {
                //编辑收支
                Item item = sortList.get((Integer)view.getTag());
                Intent intent = new Intent(MainActivity.this,SetDataActivity.class);
                intent.putExtra("isEdit",true);
                intent.putExtra("Item",item);
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
                //更新源数据
                for(Item item:itemList) {
                    if (item.getId() == i) {
                        //更新总收支
                        allChange(false,item.getTimeC(),Double.parseDouble(item.getAmount()),item.isState());
                        itemList.remove(item);
                        break;
                    }
                }

                //更新总收支显示
                allShowChange(sortIOId);
                showHideHint(adapter.getItemCount());
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
        sortList.addAll(itemList);

        //初始化总收入
        for(Item item:sortList){
            allChange(true,item.getTimeC(),Double.parseDouble(item.getAmount()),item.isState());
        }
        allShowChange(sortIOId);
    }

    private void initWidget(){
        fab = findViewById(R.id.fab);
        toolbar = findViewById(R.id.toolbar);
        params = (AppBarLayout.LayoutParams)toolbar.getLayoutParams();
        checkAllBox = findViewById(R.id.check_all);
        recyclerView = findViewById(R.id.recycler_view);
        labelTV = findViewById(R.id.checked_tv);
        yearTV = findViewById(R.id.year_tv);
        monthTV = findViewById(R.id.month_tv);
        dayTV = findViewById(R.id.day_tv);

        IOSpinner = findViewById(R.id.i_o_spinner);
        spinnerList.add("全部");
        spinnerList.add("收入");
        spinnerList.add("支出");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,R.layout.spinner_item,spinnerList);
        IOSpinner.setAdapter(spinnerAdapter);
        IOSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if(sortIOId == position)
                    return;
                sortIOId = position;
                allShowChange(sortIOId);
                sortById(sortIOId, sortDateId, pickerDate);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        helpDialog = new Dialog(this);
        View contentView = LayoutInflater.from(this).inflate(R.layout.layout_help,new LinearLayout(this),false);
        helpDialog.setContentView(contentView);
        helpDialog.setTitle("记账本-帮助");
        helpDialog.create();
    }

    //更新总收支
    //params:是否增加，收支日期，金额，收入or支出
    private void allChange(boolean isAdd, Calendar date, double amount, boolean state){
        Calendar current = Calendar.getInstance();
        if(isAdd){
            if(date.get(Calendar.YEAR) == current.get(Calendar.YEAR)) {
                yAll[state ? 0 : 1] += amount;
                if (date.get(Calendar.MONTH) == current.get(Calendar.MONTH)) {
                    mAll[state ? 0 : 1] += amount;
                    if (date.get(Calendar.DAY_OF_MONTH) == current.get(Calendar.DAY_OF_MONTH))
                        dAll[state ? 0 : 1] += amount;
                }
            }
        }
        else{
            if(date.get(Calendar.YEAR) == current.get(Calendar.YEAR)) {
                yAll[state ? 0 : 1] -= amount;
                if (date.get(Calendar.MONTH) == current.get(Calendar.MONTH)) {
                    mAll[state ? 0 : 1] -= amount;
                    if (date.get(Calendar.DAY_OF_MONTH) == current.get(Calendar.DAY_OF_MONTH))
                        dAll[state ? 0 : 1] -= amount;
                }
            }
        }
    }

    //更新总收支的显示
    private void allShowChange(int IOId){
        if(IOId == 0){
            //全部
            StringBuilder builder = new StringBuilder();
            builder.append("今年收支差额\n");
            builder.append(yAll[0]+yAll[1]);
            yearTV.setText(builder);

            builder.delete(0,builder.length());
            builder.append("今月收支差额\n");
            builder.append(mAll[0]+mAll[1]);
            monthTV.setText(builder);

            builder.delete(0,builder.length());
            builder.append("今天收支差额\n");
            builder.append(dAll[0]+dAll[1]);
            dayTV.setText(builder);
        }
        else{
            //收入，支出
            StringBuilder builder = new StringBuilder();
            builder.append(IOId==1?"今年收入总额\n":"今年支出总额\n");
            builder.append(IOId==1?yAll[1]:yAll[0]);
            yearTV.setText(builder);

            builder.delete(0,builder.length());
            builder.append(IOId==1?"今月收入总额\n":"今月支出总额\n");
            builder.append(IOId==1?mAll[1]:mAll[0]);
            monthTV.setText(builder);

            builder.delete(0,builder.length());
            builder.append(IOId==1?"今天收入总额\n":"今天支出总额\n");
            builder.append(IOId==1?dAll[1]:dAll[0]);
            dayTV.setText(builder);
        }
    }

    //分类
    private void sortById(int IOId, int DateId, Date date) {
        //DateId：0全部，1年，2月，3日
        //IOId：0全部，1支出，2收入
        sortList.clear();
        //按格式化后的字符串比较
        String dateS = null;
        SimpleDateFormat sdf = null;
        if (DateId == 1)
            sdf = new SimpleDateFormat("yyyy", Locale.CHINA);
        else if (DateId == 2)
            sdf = new SimpleDateFormat("yyyy-MM", Locale.CHINA);
        else if (DateId == 3)
            sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        if(sdf != null)
            dateS = sdf.format(date);

        //先从源列表据IOId筛选到sortList中
        if(IOId == 0) {
            sortList.addAll(itemList);
        }
        else if(IOId == 1) {
            for (Item item : itemList) {
                if (!item.isState()) {
                    sortList.add(item);
                }
            }
        }
        else if(IOId == 2){
            for(Item item:itemList)
                if(item.isState()) {
                    sortList.add(item);
                }
        }

        Iterator<Item> iterator = sortList.iterator();
        //再从sortList中去除不需要的
        if(dateS != null) {
            while (iterator.hasNext()) {
                if (!dateS.equals(sdf.format(iterator.next().getTimeD())))
                    iterator.remove();
            }
        }


        adapter.notifyDataSetChanged();
        showHideHint(adapter.getItemCount());
    }

    //多选模式下（onClick）
    public void checkBoxOnClick(View view) {
        View checkedView = view.findViewById(R.id.checked);
        Item item = sortList.get((Integer) view.findViewById(R.id.unchecked).getTag());

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
        if(mCheckCount == sortList.size()){
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
            for (Item item : sortList) {
                item.setChecked(true);
            }
            mCheckCount = sortList.size();
            fab.setEnabled(true);
        }
        else{
            for (Item item : sortList) {
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
    public void showHideHint(int count){
        if (count == 0) {
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
        //隐藏与显示
        checkAllBox.setVisibility(View.GONE);
        showHideMenu(true);
        labelTV.setVisibility(View.GONE);
        IOSpinner.setVisibility(View.VISIBLE);
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
            //fabDelete后，更新recyclerView视图交给多选删除处理
        if(!isFromFabDelete)
            adapter.notifyDataSetChanged();
        //隐藏全部toolbar中选择图标
        CheckBox checkBox = findViewById(R.id.check_all);
        checkBox.setVisibility(View.INVISIBLE);
        checkBox.setChecked(false);

        //多选删除处理
        if (isFromFabDelete){
            Item itemToDel;
            for(int i = 0;i < sortList.size();i++){
                itemToDel = sortList.get(i);
                if(itemToDel.isChecked()) {//被选中的
                    //删除数据库数据
                    LitePal.delete(Item.class,itemToDel.getId());
                    //更新收支总额
                    allChange(false,itemToDel.getTimeC(),Double.parseDouble(itemToDel.getAmount()),itemToDel.isState());
                    itemList.remove(itemToDel);
                    sortList.remove(i);
                    i--;//以防漏掉被删除后的一项未处理和越界
                    //通知adapter更新数据和界面
                    adapter.notifyItemRemoved(i+1);
                }
            }
            //更新收支总额显示
            allShowChange(sortIOId);
            adapter.notifyItemRangeChanged(0,itemList.size());
            showHideHint(adapter.getItemCount());
        }
        for(Item item:itemList){
            item.setChecked(false);
        }
        mCheckCount = 0;
    }

    //进入多选模式
    private void enterCheckedState(){
        //隐藏与显示
        showHideMenu(false);
        checkAllBox.setVisibility(View.VISIBLE);
        IOSpinner.setVisibility(View.GONE);
        labelTV.setVisibility(View.VISIBLE);
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

        //新建或编辑完成
        if (data != null && data.getBooleanExtra("isOK",false)){
            Item item = (Item)data.getSerializableExtra("Item");
            if(requestCode == 10){
                //编辑完成
                Item preItem = sortList.get(editIndex);
                //删除原来的收支
                allChange(false,preItem.getTimeC(),Double.parseDouble(preItem.getAmount()),preItem.isState());
                //总收支变化
                allChange(true,item.getTimeC(),Double.parseDouble(item.getAmount()),item.isState());
                //更新总收支显示
                allShowChange(sortIOId);
                //对源数据的元素替换
                itemList.set(itemList.indexOf(preItem),item);
                adapter.notifyItemChanged(editIndex);

            }
            else if(requestCode == 20){
                //新建完成
                //总收支变化
                allChange(true,item.getTimeC(),Double.parseDouble(item.getAmount()),item.isState());
                //更新总收支显示
                allShowChange(sortIOId);
                //对源数据的元素进行添加
                itemList.add(0,item);
                itemList.get(0).save();
            }
            //保持原来分类状态并更新视图
            sortById(sortIOId,sortDateId,pickerDate);
        }
    }

    void showHideMenu(boolean isShowOrHide){
        if(isShowOrHide) {
            for(int i = 0;i < 4;i++)
                menu.getItem(i).setVisible(true);
        }
        else
            for(int i = 0;i < 4;i++)
                menu.getItem(i).setVisible(false);
    }

    //帮助popupWindow
    public void onHelpClick(View view) {
        helpDialog.show();
    }
}

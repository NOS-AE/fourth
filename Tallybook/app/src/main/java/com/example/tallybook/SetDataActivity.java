package com.example.tallybook;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.blogspot.atifsoftwares.animatoolib.Animatoo;

import java.text.Format;
import java.util.Calendar;
import java.util.Set;

public class SetDataActivity extends AppCompatActivity {

    private boolean isInOrOut = true;//true：收入  false：支出

    private final int PRECISION = 2,
        DIGITS = 7;//PRECISION小数位数限制  DIGITS整数位限制

    private String strForET;

    private RecyclerView recyclerView;

    private PopupAdapter adapter;

    private PopupWindow popupWindow,popupWindow2,popupWindow3;//分类窗口,日期窗口,时间窗口

    private int category;

    DatePicker datePicker;//日期

    TimePicker timePicker;//时间

    private EditText amountET = null, remarkET = null;//金额ET（最大金额9 999 999.99）

    private TextView inTV = null, outTV = null,
        categoryTV = null, timeTV = null;//收入，支出的选择，分类选择

    private Item itemToEdit;

    private boolean isEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_data);

        //本活动控件初始化
        initWidget();
        //金额限制小数位数及其它限制
        setPrecision();
        //金额ET失去焦点或获得焦点时格式化其文本
        formatET();
        //Adapter和RecyclerView和PopupWindow的初始化设置
        initRecyclerView();
        //初始化PopupWindow
        initPopupWindow();

        Intent intent = getIntent();
        //判断为新建还是编辑
        isEdit = intent.getBooleanExtra("isEdit",false);
        if(isEdit){
            //再次初始化控件为需要编辑的Item
            itemToEdit = (Item)intent.getSerializableExtra("Item");
            initWidgetAgain(itemToEdit);
        }

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认");
        builder.setMessage("是否放弃编辑");
        builder.setPositiveButton("是", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent result = new Intent();
                result.putExtra("isOK",false);
                setResult(1,result);
                SetDataActivity.super.onBackPressed();
                Animatoo.animateZoom(SetDataActivity.this);
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //什么都不做，继续编辑
            }
        }).show();
    }

    //放弃添加或编辑条目
    public void setDataCancel(View view) {
        onBackPressed();
    }

    //确认条目添加或编辑完成（右上角确认点击事件）
    public void setDataConfirm(View view) {
        if(isEdit){
            //传给Main：Main传进的Item
            Intent data = new Intent();
            itemToEdit.setState(isInOrOut);
            if (amountET.getText().toString().isEmpty())
                itemToEdit.setAmount(0.0);
            else
                itemToEdit.setAmount(Double.parseDouble(amountET.getText().toString()));
            itemToEdit.setCategory(category);
            itemToEdit.setTime(timeTV.getText().toString());
            if (remarkET.getText().toString().equals(""))
                itemToEdit.setRemark("无备注");
            else
                itemToEdit.setRemark(remarkET.getText().toString());
            data.putExtra("Item",itemToEdit);
            data.putExtra("isOK",true);
            setResult(0,data);
            finish();
        }
        else{
            //传给Main：新Item
            Intent data = new Intent();
            Item item = new Item();
            item.setState(isInOrOut);
            if (amountET.getText().toString().isEmpty())
                item.setAmount(0.0);
            else
                item.setAmount(Double.parseDouble(amountET.getText().toString()));
            item.setCategory(category);
            item.setTime(timeTV.getText().toString());
            if (remarkET.getText().toString().equals(""))
                item.setRemark("无备注");
            else
                item.setRemark(remarkET.getText().toString());
            data.putExtra("Item",item);
            data.putExtra("isOK",true);
            setResult(0,data);
            finish();
        }
    }

    private void initWidget(){
        inTV = findViewById(R.id.in);
        //初始化选择为支出（xml中）
        outTV = findViewById(R.id.out);
        Drawable drawable,drawable2;//临时Drawable
        //EditText添加图标
        //金额
        amountET = findViewById(R.id.amount_et);
        drawable = ContextCompat.getDrawable(this,R.drawable.amount_icon);
        if (drawable != null)
            drawable.setBounds(0,0,100,100);
        amountET.setCompoundDrawables(drawable,null,null,null);

        //备注
        remarkET = findViewById(R.id.remarks_et);
        drawable = ContextCompat.getDrawable(this,R.drawable.remark_icon);
        if (drawable != null)
            drawable.setBounds(0,0,100,100);
        remarkET.setCompoundDrawables(drawable,null,null,null);


        //TextView添加图标
        //类别
        categoryTV = findViewById(R.id.category_tv);
        //左图标
        drawable = ContextCompat.getDrawable(this,R.drawable.category_icon);
        if (drawable != null)
            drawable.setBounds(0,0,100,100);
        //右图标
        drawable2 = ContextCompat.getDrawable(this,R.drawable.arrow_icon);
        if (drawable2 != null)
            drawable2.setBounds(0,0,40,40);
        categoryTV.setCompoundDrawables(drawable,null,drawable2,null);

        //时间
        timeTV = findViewById(R.id.time_tv);
        drawable = ContextCompat.getDrawable(this,R.drawable.time_icon);
        if(drawable != null)
            drawable.setBounds(0,0,100,100);
        timeTV.setCompoundDrawables(drawable,null,null,null);
    }

    private void initWidgetAgain(Item item){
        //将传入的数据显示
        categoryTV.setText(item.getCategory());
        amountET.setText(item.getAmount().substring(1));
        timeTV.setText(item.getTime());
        remarkET.setText(item.getRemark());
    }

    private void initRecyclerView(){
        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PopupAdapter(Item.getCategoryList(isInOrOut));
        //item点击（选择item）
        adapter.setOnItemClickListener(new PopupAdapter.OnItemClickListener() {
            @Override
            public void onClick(int i) {
                String content = Item.getCategoryList(isInOrOut).get(i);
                category = i;
                categoryTV.setText(content);
                popupWindow.dismiss();
            }
        });
        recyclerView.setAdapter(adapter);
    }

    public void initPopupWindow(){
        View view;//临时
        LinearLayout layout = new LinearLayout(this);//临时
        //选择分类
        popupWindow = new PopupWindow(recyclerView, ViewGroup.LayoutParams.WRAP_CONTENT, 830,true);
        popupWindow.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.popup_backgroud));
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                popupShadowHide();
            }
        });

        //选择日期
        view = LayoutInflater.from(this).inflate(R.layout.layout_date,layout,false);
        popupWindow2 = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,true);
        popupWindow2.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.popup_td_background));
        popupWindow2.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                popupShadowHide();
            }
        });
        popupWindow2.setAnimationStyle(R.style.Animation_Design_BottomSheetDialog);
        datePicker = view.findViewById(R.id.date_picker);

        //选择时间
        view = LayoutInflater.from(this).inflate(R.layout.layout_time,layout,false);
        popupWindow3 = new PopupWindow(view,LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,true);
        popupWindow3.setBackgroundDrawable(ContextCompat.getDrawable(this,R.drawable.popup_td_background));
        popupWindow3.setAnimationStyle(R.style.Animation_Design_BottomSheetDialog);
        popupWindow3.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                popupShadowHide();
            }
        });
        timePicker = view.findViewById(R.id.time_picker);
        timePicker.setIs24HourView(true);


        //可选时间范围的限制（当日--前2年1.1）
        datePicker.setMaxDate(System.currentTimeMillis());
        Calendar minDate = Calendar.getInstance();
        minDate.add(Calendar.YEAR,-2);
        minDate.set(Calendar.MONTH,0);
        minDate.set(Calendar.DAY_OF_MONTH,1);
        datePicker.setMinDate(minDate.getTimeInMillis());

    }

    public void onInClick(View view) {
        //Click前为Out
        if(isInOrOut){
            inTV.setBackground(ContextCompat.getDrawable(this,R.drawable.shape_in_out_select));
            inTV.setTextColor(ContextCompat.getColor(this,R.color.white));
            outTV.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_in_out));
            outTV.setTextColor(ContextCompat.getColor(this,R.color.colorAccent));
            isInOrOut = false;

            //更换分类列表
            adapter.setList(Item.getCategoryList(false));
            adapter.notifyDataSetChanged();
        }
    }

    public void onOutClick(View view) {
        //Click前为In
        if(!isInOrOut){
            inTV.setBackground(ContextCompat.getDrawable(this,R.drawable.shape_in_out));
            inTV.setTextColor(ContextCompat.getColor(this,R.color.colorAccent));
            outTV.setBackground(ContextCompat.getDrawable(this, R.drawable.shape_in_out_select));
            outTV.setTextColor(ContextCompat.getColor(this,R.color.white));
            isInOrOut = true;

            //更换分类列表
            adapter.setList(Item.getCategoryList(true));
            adapter.notifyDataSetChanged();
        }
    }

    //对金额进行输入限制
    private void setPrecision(){
        amountET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                strForET = s.toString();
                //限制小数位数
                if (s.toString().contains(".")){
                    if(s.length() - 1 - s.toString().indexOf(".") > PRECISION){
                        s = s.toString().subSequence(0,s.toString().indexOf(".")+PRECISION+1);
                        amountET.setText(s);
                        amountET.setSelection(s.length());
                    }
                }
                //据小数点是否存在以继续输入小数位数
                else if(s.length() > DIGITS){
                    s = s.toString().subSequence(0,DIGITS);
                    amountET.setText(s);
                    amountET.setSelection(s.length());
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                strForET = s.toString();
                //禁止无整数情况下输入小数点
                if(s.length() == 1 && strForET.contains(".")){
                    amountET.setText("");
                }
                //整数禁止输入无效0
                else if(s.length() == 2 && s.charAt(0) == '0' &&
                    s.charAt(1) != '.'){
                    amountET.setText("0");
                    amountET.setSelection(1);
                }
            }
        });
    }

    //对金额格式化
    private void formatET(){
        amountET.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(final View v, boolean hasFocus) {
                strForET = amountET.getText().toString();
                if(hasFocus){
                    //??
                    //获得焦点时全选
                    v.post(new Runnable() {
                        @Override
                        public void run() {
                            amountET.selectAll();
                        }
                    });
                }
                else{
                    //失去焦点时格式化
                    if (strForET.contains(".")){
                        int pre = strForET.length() - strForET.indexOf(".") - 1;//小数位数
                        if(pre == 0)
                            strForET += "00";
                        else if(pre == 1)
                            strForET += "0";

                    }
                    else if(strForET.length() == 0)
                        strForET += "0.00";
                    else
                        strForET += ".00";
                    amountET.setText(strForET);
                }
            }
        });
    }

    //点击分类TV（onClick）
    public void onCategoryClick(View view) {
        popupWindow.showAsDropDown(view,0,0,Gravity.END);
        popupShadowShow();
    }

    @Override
    protected void onDestroy() {
        Log.d("MainActivity","onDestroy");
        super.onDestroy();
    }

    //点击时间（onClick）
    public void setTimeClick(View view) {
        popupWindow2.showAtLocation(this.getWindow().getDecorView(), Gravity.CENTER,0,0);
        popupShadowShow();
    }

    //
    private void popupShadowShow(){
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.alpha = 0.7f;
        this.getWindow().setAttributes(lp);
    }

    private void popupShadowHide(){
        WindowManager.LayoutParams lp = SetDataActivity.this.getWindow().getAttributes();
        lp.alpha = 1f;
        SetDataActivity.this.getWindow().setAttributes(lp);
    }

    //设置Date完成
    public void onDateOkClick(View view) {
        //关闭日期设置
        popupWindow2.dismiss();
        //开启时间设置
        popupWindow3.showAtLocation(getWindow().getDecorView(),Gravity.CENTER,0,0);
    }

    //完成时间设定（onClick）
    public void onTimeOkClick(View view) {
        popupWindow3.dismiss();
        StringBuilder builder = new StringBuilder();
        builder.append(datePicker.getYear());
        builder.append('-');
        builder.append(datePicker.getMonth());
        builder.append('-');
        builder.append(datePicker.getDayOfMonth());
        builder.append("  ");
        builder.append(timePicker.getHour());
        builder.append(':');
        builder.append(timePicker.getMinute());
        timeTV.setText(builder);
    }

}

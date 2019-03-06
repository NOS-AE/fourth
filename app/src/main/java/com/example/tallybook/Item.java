package com.example.tallybook;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Item extends LitePalSupport implements Serializable {
    @Column(ignore = true)
    private static final long serialVersionUID = 1L;
    private int id;//ItemList表主键
    private double amount = 0.0;//金额
    private boolean state = true;//true：支出  false：收入
    private int category = 0;//分类
    private String remark = "Nothing for test for test";//备注
    private Date time;//时间
    @Column(ignore = true)
    private boolean isChecked = false;//供长按状态是否选中判断
    @Column(ignore = true)
    private static List<String> categoryIn = new ArrayList<>();
    @Column(ignore = true)
    private static List<String> categoryOut = new ArrayList<>();

    boolean isChecked() {
        return isChecked;
    }

    void setChecked(boolean checked) {
        isChecked = checked;
    }

    Item(){
        //设为当前时间，并格式化
        time = new Date();
    }

    int getId() {
        return id;
    }

    void setAmount(double amount) {
        this.amount = amount;
    }

    void setState(boolean state) {
        this.state = state;
    }

    String getAmount() {
        return (state?"-":"+") + String.format(Locale.getDefault(),"%.2f", amount);
    }

    void setTime(Date time) {
        this.time = time;
    }

    boolean isState() {
        return state;
    }

    String getCategory() {
        if(state)
            return categoryOut.get(category);
        return categoryIn.get(category);
    }

    int getCategoryI(){
        return category;
    }

    void setCategory(int category) {
        this.category = category;
    }

    String getRemark() {
        return remark;
    }

    void setRemark(String remark) {
        this.remark = remark;
    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
        return sdf.format(time);
    }

    Date getTimeD(){
        return time;
    }

    Calendar getTimeC(){
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        return c;
    }

    static List<String> getCategoryList(boolean state){
        if(state)
            return categoryOut;
        return categoryIn;
    }

    static void initCategory(){
        categoryOut.clear();
        categoryIn.clear();

        categoryOut.add("其它支出");
        categoryOut.add("餐饮买菜");
        categoryOut.add("零食饮料");
        categoryOut.add("交通");
        categoryOut.add("衣服鞋帽");
        categoryOut.add("日用品");
        categoryOut.add("通讯网费");
        categoryOut.add("休闲娱乐");
        categoryOut.add("医疗");
        categoryOut.add("学习");
        categoryOut.add("烟酒");
        categoryOut.add("家居");
        categoryOut.add("护肤彩妆");
        categoryOut.add("住房");
        categoryOut.add("数码");
        categoryOut.add("宠物");

        categoryIn.add("其他收入");
        categoryIn.add("工资");
        categoryIn.add("生活费");
        categoryIn.add("红包");
        categoryIn.add("兼职外快");
        categoryIn.add("投资收入");
        categoryIn.add("奖金");
    }
}

package com.example.tallybook;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.litepal.LitePal;

import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder>
    implements ItemTouchHelperAdapter {
    //收支List
    private List<Item> mItemList;
    //MainActivity Context
    private Context mContext;
    //被触摸的子item的View
    private View mOnClickView;
    //是否多选状态
    private boolean isChecked = false;

    private OnItemClickListener mOnItemClickListener = null;

    private StateChecker mStateChecker = null;

    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView remark, category, time, amount;
        ViewHolder(View view){
            super(view);
            remark = view.findViewById(R.id.remarks_tv);
            category = view.findViewById(R.id.category_tv);
            time = view.findViewById(R.id.time_tv);
            amount = view.findViewById(R.id.amount_tv);
        }
    }

    ItemAdapter(List<Item> itemList, Context context)
    {
        mItemList = itemList;
        mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item,viewGroup,false);
        //layout.setRippleInAdapter(true);
        //为子item注册回调
        view.setFocusable(true);
        view.setClickable(true);
        //view注册点击事件
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnClickView = v;
                if(mOnItemClickListener != null) {
                    if (isChecked)
                        mOnItemClickListener.onCheck(mOnClickView);
                    else {

                        mOnItemClickListener.onClick(mOnClickView);
                    }
                }
            }
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        Item item = mItemList.get(i);
        viewHolder.remark.setText(item.getRemark());
        viewHolder.category.setText(item.getCategory());
        viewHolder.time.setText(item.getTime());
        viewHolder.amount.setText(item.getAmount());
        //随着删除与添加的存在，在onBindViewHolder中动态为子item的view setTag
        viewHolder.itemView.setTag(i);
        //在灰色小圆点（unchecked view）setTag，以便MainActivity中多选时索引
        viewHolder.itemView.findViewById(R.id.unchecked).setTag(i);
        //防view复用造成checkbox(自定义)的显示错误
        if(mItemList.get(i).isChecked())
            viewHolder.itemView.findViewById(R.id.checked).setVisibility(View.VISIBLE);
        else
            viewHolder.itemView.findViewById(R.id.checked).setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    @Override
    public void onItemDismiss(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("确认");
        builder.setMessage("是否确认删除此收支");
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int tempId = mItemList.get(position).getId();
                //更新数据库
                LitePal.delete(Item.class,tempId);

                mItemList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(0,getItemCount());
                //数据如被清除，则由MainActivity更新视图
                if (mStateChecker != null){
                    mStateChecker.onCheck(tempId);//传入id，是否源数据，据id删除源数据
                }
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                notifyItemChanged(position);
            }
        });
        //点击dialog以外空白区域或返回键
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                notifyItemChanged(position);
            }
        });

        builder.show();
    }

    void setOnItemClickListener(OnItemClickListener onItemClickListener){
        mOnItemClickListener = onItemClickListener;
    }

    //由MainActivity启动SetDataActivity
    interface OnItemClickListener{
        void onClick(View view);//普通点击
        void onCheck(View view);//选择
    }

    void setOnStateChecker(StateChecker stateChecker){
        mStateChecker = stateChecker;
    }

    interface StateChecker{
        void onCheck(int i);//查看是否数据已被删除完，是则更新主页面视图
    }

    void setChecked(boolean checked) {
        isChecked = checked;
    }

}

package cn.com.wuxiang.bluetoothchat.adapter;

import android.content.Context;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import cn.com.wuxiang.bluetoothchat.R;


/**
 * Created by wuxiang on 16-10-27.
 */

public class BluetoothDeviceAdapter extends BaseAdapter{
    private Context mContext;
    private List<Map<String,Object>> mList;
    public BluetoothDeviceAdapter(Context context, List<Map<String,Object>> list){
        mContext = context;
        mList = list;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
       /* if("已配对"==(mList.get(position).get("state").toString())){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.layout_head_device_paired,null);
            return convertView;
        }
        else if("未配对"==(mList.get(position).get("state").toString())){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.layout_head_device_not_paired,null);
            return convertView;
        }*/

        ViewHolder viewHolder = null;
        if(convertView == null){
            viewHolder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.bt_list_layout,null);
            viewHolder.name = (TextView)convertView.findViewById(R.id.name);
            viewHolder.address = (TextView)convertView.findViewById(R.id.address);
            viewHolder.state = (TextView)convertView.findViewById(R.id.state);
            convertView.setTag(viewHolder);
        }
        else{
            viewHolder = (ViewHolder)convertView.getTag();
        }
        if(mList.size()>0){
            viewHolder.name.setText(mList.get(position).get("name")+"");
            viewHolder.address.setText(mList.get(position).get("address")+"");
            viewHolder.state.setText(mList.get(position).get("state")+"");
        }
        return convertView;
    }
    class ViewHolder{
        private TextView name;
        private TextView state;
        private TextView address;
    }
}

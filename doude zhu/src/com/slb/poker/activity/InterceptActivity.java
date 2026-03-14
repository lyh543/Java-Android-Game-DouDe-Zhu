package com.slb.poker.activity;

import com.slb.poker.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout.LayoutParams;

public class InterceptActivity extends NavigationMainActivity{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play);
	}
	@Override
	protected void onStart() {
		super.onStart();
		showCommonDialogMessage("\u60a8\u5df2\u7ecf\u9000\u51fa\u6e38\u620f\u4e86");
	}
	@Override
	protected void onStop() {
		dialog.dismiss();
		super.onStop();
	}
	AlertDialog dialog =  null;
	private void showCommonDialogMessage(String message){
		AlertDialog.Builder normalDialog = new AlertDialog.Builder(InterceptActivity.this);
	           normalDialog.setTitle("提醒");
	           normalDialog.setMessage(message);
	           normalDialog.setPositiveButton("确定", 
	               new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int which) {
	            	   startToAnotherActivity(BoardActivity.class);
	               }
	           });
	           normalDialog.setNegativeButton("关闭", 
	               new DialogInterface.OnClickListener() {
	               @Override
	               public void onClick(DialogInterface dialog, int which) {
	            	   
	               }
	           });
	           normalDialog.setCancelable(false);
	     dialog = normalDialog.show();
    }
	@Override
	protected void ViewAfterShow(int viewID, int width, int height) {
	}
	@Override
	protected void addView(View view, LayoutParams params) {
	}
	@Override
	protected void removeView(View view) {
	}
}

package com.example.sample66;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class SettingBaseActivity extends Activity {

	@ Override
	public boolean dispatchTouchEvent ( MotionEvent ev ) {
		if ( ev.getAction ( ) == MotionEvent.ACTION_DOWN ) {
			View v = getCurrentFocus ( );
			if ( isShouldHideInput ( v , ev ) ) {
				
				InputMethodManager imm = ( InputMethodManager ) getSystemService ( Context.INPUT_METHOD_SERVICE );
				if ( imm != null ) {
					imm.hideSoftInputFromWindow ( v.getWindowToken ( ) , 0 );
				}
			}
			return super.dispatchTouchEvent ( ev );
		}
		// 必不可少，否则所有的组件都不会有TouchEvent了
		if ( getWindow ( ).superDispatchTouchEvent ( ev ) ) {
			return true;
		}
		return onTouchEvent ( ev );
	}
	
	public boolean isShouldHideInput ( View v , MotionEvent event ) {
		if ( v != null && ( v instanceof EditText ) ) {
			int [ ] leftTop = { 0 , 0 };
			// 获取输入框当前的location位置
			v.getLocationInWindow ( leftTop );
			int left = leftTop [ 0 ];
			int top = leftTop [ 1 ];
			int bottom = top + v.getHeight ( );
			int right = left + v.getWidth ( );
			if ( event.getX ( ) > left && event.getX ( ) < right && event.getY ( ) > top && event.getY ( ) < bottom ) {
				// 点击的是输入框区域，保留点击EditText的事件
				return false;
			} else {
				return true;
			}
			
		}
		return false;
	}
	

}

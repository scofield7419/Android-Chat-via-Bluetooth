package com.example.sample66;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MyArrayAdapter extends ArrayAdapter < Msg > {
	private int resource;
	
	public MyArrayAdapter ( Context context , int resource ,List < Msg > obj) {
	        super ( context , resource ,obj);
	        this.resource = resource;
        }
	
	@ Override
	public View getView ( int position , View convertView , ViewGroup parent ) {
	        Msg msg = getItem ( position );
	        View view ;
	        VeiwHolder viewHolder;
	        if(convertView == null){
	        	view = LayoutInflater.from ( getContext ( ) ).inflate (resource , null );
	        	viewHolder = new VeiwHolder ( );
	        	viewHolder.leftLayout = ( LinearLayout ) view.findViewById ( R.id.left_layout );
	        	viewHolder.rightLayout = ( LinearLayout ) view.findViewById ( R.id.right_layout );
	        	viewHolder.leftText = ( TextView ) view.findViewById ( R.id.left_text );
	        	viewHolder.rightText = ( TextView ) view.findViewById ( R.id.right_text );
	        	view.setTag ( viewHolder );
	        	
	        }else{
	        	view = convertView;
	        	viewHolder = ( VeiwHolder ) view.getTag ( );
	        }
	        
	        if(msg.getType ( ) == Msg.RECEIVED_TYPE){
	        	viewHolder.leftLayout.setVisibility(View.VISIBLE);
	        	viewHolder.rightLayout.setVisibility ( View.GONE );
	        	viewHolder.leftText.setText ( msg.getContent ( ) );
	        }else{
	        	viewHolder.leftLayout.setVisibility(View.GONE);
	        	viewHolder.rightLayout.setVisibility ( View.VISIBLE );
	        	viewHolder.rightText.setText ( msg.getContent ( ) );
	        }
	        return view;
	}
	
	class VeiwHolder{
		LinearLayout leftLayout;
		LinearLayout rightLayout;
		TextView leftText;
		TextView rightText;
		
	}
}

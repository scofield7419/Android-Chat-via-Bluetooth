package com.example.sample66;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ChatActivity extends SettingBaseActivity {
	ListView list;
	EditText editText;
	Button sendBtn;
	MyArrayAdapter adapter;
	List < Msg > msgs;
	
	ConnectedThread connectedThread;
	
	boolean connectedThreadIsAlive;
	
	Handler chatHandler = new Handler ( ) {
		public void handleMessage ( Message msg ) {
			switch ( msg.what ) {
				case 0x118 :
					// 收到消息显示出来
					// msgs.add ( ( String ) msg.obj );
					String msg_received = msg.getData ( ).getString ( "msg_received" );
					Msg msgEntityReceived = new Msg ( msg_received , Msg.RECEIVED_TYPE );
					msgs.add ( msgEntityReceived );
					adapter.notifyDataSetChanged ( );
					list.setSelection ( msgs.size ( ) );
					break;
				case 0x117 :
					String msg_send = msg.getData ( ).getString ( "msg_send" );
					Msg msgEntitySend = new Msg ( msg_send , Msg.SEND_TYPE );
					msgs.add ( msgEntitySend );
					adapter.notifyDataSetChanged ( );
					list.setSelection ( msgs.size ( ) );
					break;
				default :
					break;
			}
			adapter.notifyDataSetChanged ( );
			list.setSelection ( msgs.size ( ) - 1 );
		}
	};
	
	@ Override
	protected void onCreate ( Bundle savedInstanceState ) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_chat );
		
		setTitle ( "chat with" );
		initView ( );
		initMsg ( );
		
		adapter = new MyArrayAdapter ( this , R.layout.chat_list_item , msgs );
		list.setAdapter ( adapter );
		sendBtn.setOnClickListener ( new OnClickListener ( ) {
			public void onClick ( View v ) {
				String content = editText.getText ( ).toString ( );
				if ( ! "".equals ( content ) ) {
					editText.setText ( "" );
					editText.clearFocus ( );
					InputMethodManager imm = ( InputMethodManager ) getSystemService ( Context.INPUT_METHOD_SERVICE );
					imm.hideSoftInputFromWindow ( editText.getWindowToken ( ) , 0 );
					sendMessage ( content );
				} else {
					Toast.makeText ( ChatActivity.this , "发送内容不能为空！" , Toast.LENGTH_SHORT ).show ( );
				}
				
			}
			
		} );
		
		getMyIntent ( );
	}
	
	@ Override
	public boolean onCreateOptionsMenu ( Menu menu ) {
		getMenuInflater ( ).inflate ( R.menu.chat , menu );
		return true;
	}
	
	@ Override
	public boolean onOptionsItemSelected ( MenuItem item ) {
		int id = item.getItemId ( );
		if ( id == R.id.back ) {
			finishChat ( );
			return true;
		}
		return super.onOptionsItemSelected ( item );
	}
	
	private void finishChat ( ) {
		connectedThreadIsAlive = false;
		connectedThread.cancel ( );
		chatInputStream = null;
		chatOutputStream = null;
		MainActivity.isChatting = false;
		finish ( );
	}
	
	@ Override
	public void onBackPressed ( ) {
		finishChat ( );
		super.onBackPressed ( );
	}
	
	private void getMyIntent ( ) {
		int type = getIntent ( ).getIntExtra ( "socketType" , - 1 );
		if ( type == - 1 ) {
			setResult ( RESULT_OK , new Intent ( ).putExtra ( "result" , - 1 ) );
			finish ( );
		}
		
		BluetoothSocket socketFromServer = null;
		if ( type == 0 ) {
			socketFromServer = MainActivity.socketFromServer;
		} else if ( type == 1 ) {
			socketFromServer = MainActivity.socketFromClient;
			
		}
		connectedThreadIsAlive = true;
		MainActivity.isChatting = true;
		connectedThread = new ConnectedThread ( socketFromServer );
		connectedThread.start ( );
	}
	
	private void initView ( ) {
		editText = ( EditText ) findViewById ( R.id.input_edit );
		sendBtn = ( Button ) findViewById ( R.id.send );
		list = ( ListView ) findViewById ( R.id.msg_list );
	}
	
	private void initMsg ( ) {
		msgs = new ArrayList < Msg > ( );
		msgs.add ( new Msg ( "hello man !" , Msg.RECEIVED_TYPE ) );
		msgs.add ( new Msg ( "hi there !" , Msg.SEND_TYPE ) );
		msgs.add ( new Msg ( "666" , Msg.RECEIVED_TYPE ) );
	}
	
	private void sendMessage ( String content ) {
		if ( connectedThread == null ) {
			return;
		}
		// TODO 先发给thread,再转发handler发给listview
		connectedThread.write ( content.getBytes ( ) );
		
		Bundle bundle = new Bundle ( );
		bundle.putString ( "msg_send" , content );
		Message msg = new Message ( );
		msg.what = 0x117;
		msg.setData ( bundle );
		chatHandler.sendMessage ( msg );
	}
	
	private void receivedMessage ( String content ) {
		// TODO 发给listview
		Bundle bundle = new Bundle ( );
		bundle.putString ( "msg_received" , content );
		Message msg = new Message ( );
		msg.what = 0x118;
		msg.setData ( bundle );
		chatHandler.sendMessage ( msg );
	}
	
	InputStream chatInputStream = null;
	OutputStream chatOutputStream = null;
	
	public class ConnectedThread extends Thread {
		
		private final BluetoothSocket mmSocket;
		
		public ConnectedThread ( BluetoothSocket socket ) {
			mmSocket = socket;
			try {
				chatInputStream = socket.getInputStream ( );
				chatOutputStream = socket.getOutputStream ( );
			} catch ( IOException e ) {
				e.printStackTrace ( );
			}
		}
		
		public void run ( ) {
			// buffer store for the stream
			byte [ ] buffer = new byte [ 1024 ];
			// bytes returned from read()
			int bytes;
			while ( connectedThreadIsAlive ) {
				try {
					
					if ( ( bytes = chatInputStream.read ( buffer ) ) > 0 ) {
						byte [ ] buf = new byte [ bytes ];
						for ( int i = 0 ; i < bytes ; i ++ ) {
							buf [ i ] = buffer [ i ];
						}
						receivedMessage ( new String ( buf ) );
					}
					
				} catch ( IOException e ) {
					e.printStackTrace ( );
				}
			}
			
		}
		
		public void write ( byte [ ] bytes ) {
			if ( chatOutputStream == null ) {
				Toast.makeText ( ChatActivity.this , "没有连接" , Toast.LENGTH_SHORT ).show ( );
				System.out.println ( "没有连接```ConnectedThread```write" );
				return;
			}
			try {
				chatOutputStream.write ( bytes );
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace ( );
			}
		}
		
		/* Call this from the main Activity to shutdown the connection */
		public void cancel ( ) {
			try {
				mmSocket.close ( );
				chatInputStream.close ( );
				chatOutputStream.close ( );
			} catch ( IOException e ) {
				e.printStackTrace ( );
			}
		}
	}
}

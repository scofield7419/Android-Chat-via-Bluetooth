package com.example.sample66;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	public static final UUID S_UUID = UUID.fromString ( "00001101-0000-1000-8000-00805F9B34FB" );
	public static final String S_NAME = "BluetoothChat";
	
	public static final int SOCKET_TYPE_SERVER = 0;
	public static final int SOCKET_TYPE_CLIENT = 1;
	
	TextView address;
	ToggleButton toggle;
	ToggleButton toggle_scan;
	ToggleButton toggle_visible;
	ListView list_bt;
	
	BluetoothAdapter bluetoothAdapter;
	
	ArrayList < BluetoothDevice > pairedDevicesList;
	
	ArrayList < HashMap < String , Object > > list;
	SimpleAdapter adapter;
	
	MyBroadcastReceiver mFoundReceiver;
	
	ProgressDialog progressDialogClientConnecting;
	
	RequestAcceptThread requestAcceptThread;
	RequestThread requestThread;
	
	public static boolean isChatting;
	
	int flagType;
	
	boolean mainIsAlive;
	
	Handler transHandler = new Handler ( ) {
		public void handleMessage ( android.os.Message msg ) {
			switch ( msg.what ) {
				case 0x111 :
					
					break;
				
				case 0x112 :
					
					break;
				
				case 0x113 :
					// 显示跳转步骤，进入聊天activity，并开启线程，把接收到的socket注入给次线程
					AlertDialog.Builder dialog = new AlertDialog.Builder ( MainActivity.this );
					dialog.setTitle ( "服务端提示" );
					dialog.setMessage ( "已接收到一个聊天请求，是否接受并进入聊天?" );
					dialog.setPositiveButton ( "进入" , new OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							Intent intent = new Intent ( MainActivity.this , ChatActivity.class );
							intent.putExtra ( "socketType" , SOCKET_TYPE_SERVER );
							MainActivity.this.startActivityForResult ( intent , 0x888 );
							flagType = SOCKET_TYPE_SERVER;
						}
					} );
					dialog.setNegativeButton ( "取消" , new DialogInterface.OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							// TODO 关闭 socket
						}
					} );
					dialog.show ( );
					startCheckIsChatting ( );
					break;
				
				case 0x114 :
					
					progressDialogClientConnecting.show ( );
					break;
				
				case 0x115 :
					progressDialogClientConnecting.dismiss ( );
					AlertDialog.Builder dialog2 = new AlertDialog.Builder ( MainActivity.this );
					dialog2.setTitle ( "客户端提示" );
					dialog2.setMessage ( "聊天请求已被接受，是否开始聊天?" );
					dialog2.setPositiveButton ( "进入" , new OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							Intent intent2 = new Intent ( MainActivity.this , ChatActivity.class );
							intent2.putExtra ( "socketType" , SOCKET_TYPE_CLIENT );
							MainActivity.this.startActivityForResult ( intent2 , 0x999 );
							flagType = SOCKET_TYPE_CLIENT;
						}
					} );
					dialog2.setNegativeButton ( "放弃" , new DialogInterface.OnClickListener ( ) {
						public void onClick ( DialogInterface dialog , int which ) {
							// TODO 关闭 socket
						}
					} );
					dialog2.show ( );
					startCheckIsChatting ( );
					break;
				
				case 0x116 :
					
					break;
				
				default :
					break;
			}
		}
		
	};
	boolean requestAcceptThreadFlag;
	boolean requestThreadFlag;
	
	private void startCheckIsChatting ( ) {
		new Thread ( ) {
			public void run ( ) {
				while ( mainIsAlive ) {
					if ( ! isChatting ) {
						setChatFinishAction ( );
						break;
					}
				}
			}
		}.start ( );
		
	}
	
	@ Override
	protected void onCreate ( Bundle savedInstanceState ) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_main );
		setTitle ( "chat via bt" );
		initView ( );
		mainIsAlive = true;
		initProgressBar ( );
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter ( );
		mFoundReceiver = new MyBroadcastReceiver ( );
		
		toggle.setOnCheckedChangeListener ( new OnCheckedChangeListener ( ) {
			public void onCheckedChanged ( CompoundButton buttonView , boolean isChecked ) {
				if ( isChecked ) {
					if ( ! bluetoothAdapter.isEnabled ( ) ) {
						Intent turnOn = new Intent ( BluetoothAdapter.ACTION_REQUEST_ENABLE );
						startActivityForResult ( turnOn , 0x123 );
						Toast.makeText ( getApplicationContext ( ) , "蓝牙已开启" , Toast.LENGTH_LONG ).show ( );
					}
					if ( ! bluetoothAdapter.isDiscovering ( ) ) {
						openBlue ( );
					}
					askForOpenServerSocket ( );
				} else {
					if ( ! bluetoothAdapter.isEnabled ( ) ) {
						return;
					}
					bluetoothAdapter.disable ( );
					address.setVisibility ( View.INVISIBLE );
					list.removeAll ( list );
					adapter.notifyDataSetChanged ( );
					Toast.makeText ( getApplicationContext ( ) , "蓝牙已关闭" , Toast.LENGTH_LONG ).show ( );
					toggle_visible.setChecked ( false );
					toggle_scan.setChecked ( false );
				}
			}
			
		} );
		
		toggle_visible.setOnCheckedChangeListener ( new OnCheckedChangeListener ( ) {
			public void onCheckedChanged ( CompoundButton buttonView , boolean isChecked ) {
				if ( ! toggle.isChecked ( ) ) {
					Toast.makeText ( getApplicationContext ( ) , "请先开启蓝牙" , Toast.LENGTH_LONG ).show ( );
					toggle_visible.setChecked ( false );
					return;
				}
				if ( isChecked ) {
					Intent discoverableIntent = new Intent ( BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE );
					discoverableIntent.putExtra ( BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION , 1800 );
					startActivityForResult ( discoverableIntent , 0x124 );
				}
			}
		} );
		
		toggle_scan.setOnCheckedChangeListener ( new OnCheckedChangeListener ( ) {
			public void onCheckedChanged ( CompoundButton buttonView , boolean isChecked ) {
				if ( ! toggle.isChecked ( ) ) {
					Toast.makeText ( getApplicationContext ( ) , "请先开启蓝牙" , Toast.LENGTH_LONG ).show ( );
					toggle_scan.setChecked ( false );
					return;
				}
				if ( isChecked ) {
					startDiscovery ( );
				} else {
					if ( ! bluetoothAdapter.isEnabled ( ) ) {
						return;
					}
					bluetoothAdapter.cancelDiscovery ( );
				}
			}
		} );
		
	}
	
	private void initProgressBar ( ) {
		progressDialogClientConnecting = new ProgressDialog ( MainActivity.this );
		progressDialogClientConnecting.setTitle ( "蓝牙客户端" );
		progressDialogClientConnecting.setMessage ( "正在连接中" );
		progressDialogClientConnecting.setCancelable ( false );
		progressDialogClientConnecting.setIndeterminate ( false );
	}
	
	private void startDiscovery ( ) {
		IntentFilter filter = new IntentFilter ( BluetoothDevice.ACTION_FOUND );// 开启搜索
		registerReceiver ( mFoundReceiver , filter );
		filter = new IntentFilter ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED );// 搜索完成
		registerReceiver ( mFoundReceiver , filter );
		bluetoothAdapter.startDiscovery ( );
	}
	
	@ Override
	protected void onStop ( ) {
		mainIsAlive = false;
		// bluetoothAdapter.disable ( );
		// toggle.setChecked ( false );
		// address.setVisibility ( View.INVISIBLE );
		// list.removeAll ( list );
		// adapter.notifyDataSetChanged ( );
		super.onStop ( );
	}
	
	private void askForOpenServerSocket ( ) {
		AlertDialog.Builder dialog3 = new AlertDialog.Builder ( MainActivity.this );
		dialog3.setTitle ( "提示" );
		dialog3.setMessage ( "蓝牙已开启，是否允许接受聊天请求?" );
		dialog3.setPositiveButton ( "允许" , new OnClickListener ( ) {
			public void onClick ( DialogInterface dialog , int which ) {
				// 本机蓝牙可见后，记得在合适的时候（退出聊天）关闭线程。
				requestAcceptThread = new RequestAcceptThread ( );
				requestAcceptThread.start ( );
				requestAcceptThreadFlag = true;
			}
		} );
		dialog3.setNegativeButton ( "放弃" , null );
		dialog3.show ( );
	}
	
	@ Override
	protected void onActivityResult ( int requestCode , int resultCode , Intent data ) {
		if ( resultCode != RESULT_OK ) {
			return;
		}
		if ( requestCode == 0x123 ) {
			openBlue ( );
			// askForOpenServerSocket ( );
		} else if ( requestCode == 0x124 ) {
			Toast.makeText ( getApplicationContext ( ) , "蓝牙已可见" , Toast.LENGTH_LONG ).show ( );
			
		} else if ( requestCode == 0x888 ) {
			// ChatActivity.ConnectedThread readthread = new
			// ChatActivity.ConnectedThread ( socketFromServer );
			// readthread.start ( );
			if ( data.getIntExtra ( "result" , 0 ) == - 1 ) {
				// TODO 显示异常
			}
		} else if ( requestCode == 0x999 ) {
			if ( data.getIntExtra ( "result" , 0 ) == - 1 ) {
				// TODO 显示异常
			}
		}
		
		super.onActivityResult ( requestCode , resultCode , data );
	}
	
	private void setChatFinishAction ( ) {
		if ( requestAcceptThread != null ) {
			if ( requestAcceptThread.isAlive ( ) ) {
				requestAcceptThreadFlag = false;
			}
			requestThread = null;
		}
		if ( requestThread != null ) {
			if ( requestThread.isAlive ( ) ) {
				requestThreadFlag = false;
			}
			requestThread = null;
		}
	}
	
	private void openBlue ( ) {
		address.setVisibility ( View.VISIBLE );
		address.setText ( "本机名：" + bluetoothAdapter.getName ( ) + "\n本机地址: " + bluetoothAdapter.getAddress ( ) );
		
		Set < BluetoothDevice > pairedDevices = bluetoothAdapter.getBondedDevices ( );
		pairedDevicesList = new ArrayList < BluetoothDevice > ( );
		list = new ArrayList < HashMap < String , Object > > ( );
		for ( BluetoothDevice bt : pairedDevices ) {
			pairedDevicesList.add ( bt );
			HashMap < String , Object > map = new HashMap < String , Object > ( );
			map.put ( "name" , "设备名称：" + bt.getName ( ) );
			map.put ( "address" , "设备地址：\n" + bt.getAddress ( ) );
			map.put ( "uuid" , "设备UUID：\n" + bt.getUuids ( ) );
			map.put ( "state" , "连接状态：\n" + bt.getBondState ( ) );
			list.add ( map );
		}
		adapter = new SimpleAdapter ( MainActivity.this , list , R.layout.list_item , new String [ ] {
		                "name" , "address" , "uuid" , "state" } , new int [ ] {
		                R.id.name , R.id.address , R.id.uuid ,
		                R.id.state } );
		list_bt.setAdapter ( adapter );
		list_bt.setOnItemClickListener ( new OnItemClickListener ( ) {
			public void onItemClick ( AdapterView < ? > parent , View view , final int position , long id ) {
				AlertDialog.Builder dialog4 = new AlertDialog.Builder ( MainActivity.this );
				dialog4.setTitle ( "提示" );
				dialog4.setMessage ( "是否请求进行聊天?" );
				dialog4.setPositiveButton ( "确认" , new OnClickListener ( ) {
					public void onClick ( DialogInterface dialog , int which ) {
						// 本机蓝牙可见后，记得在合适的时候（退出聊天）关闭线程。
						BluetoothDevice device = pairedDevicesList.get ( position );
						requestThread = new RequestThread ( device );
						requestThread.start ( );
						requestThreadFlag = true;
					}
				} );
				dialog4.setNegativeButton ( "放弃" , null );
				dialog4.show ( );
			}
		} );
	}
	
	private void initView ( ) {
		address = ( TextView ) findViewById ( R.id.address );
		toggle = ( ToggleButton ) findViewById ( R.id.toggle );
		toggle_scan = ( ToggleButton ) findViewById ( R.id.toggle_scan );
		toggle_visible = ( ToggleButton ) findViewById ( R.id.toggle_visible );
		list_bt = ( ListView ) findViewById ( R.id.list_bt );
	}
	
	class MyBroadcastReceiver extends BroadcastReceiver {
		
		public void onReceive ( Context context , Intent intent ) {
			String action = intent.getAction ( );
			// 找到设备
			if ( BluetoothDevice.ACTION_FOUND.equals ( action ) ) {
				BluetoothDevice device = intent.getParcelableExtra ( BluetoothDevice.EXTRA_DEVICE );
				// 添加进一个设备列表，进行显示。
				if ( device.getBondState ( ) != BluetoothDevice.BOND_BONDED ) {
					HashMap < String , Object > map = new HashMap < String , Object > ( );
					map.put ( "name" , "设备名称：" + device.getName ( ) );
					map.put ( "address" , "设备地址：\n" + device.getAddress ( ) );
					map.put ( "uuid" , "设备UUID：\n" + device.getUuids ( ) );
					map.put ( "state" , "连接状态：\n" + device.getBondState ( ) );
					pairedDevicesList.add ( device );
					list.add ( map );
					adapter.notifyDataSetChanged ( );
				}
			}
			// 搜索完成
			else if ( BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals ( action ) ) {
				bluetoothAdapter.cancelDiscovery ( );
			}
			toggle_scan.setChecked ( false );
		}
	};
	
	public static BluetoothSocket socketFromServer = null;
	
	class RequestAcceptThread extends Thread {
		private BluetoothServerSocket m_BluetoothServersocket = null;
		
		public RequestAcceptThread ( ) {
			BluetoothServerSocket tmpBluetoothServersocket = null;
			try {
				tmpBluetoothServersocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord ( S_NAME , S_UUID );
			} catch ( IOException e ) {
				System.out.println ( "RequestAcceptThread```tmpBluetoothServersocket```" );
				e.printStackTrace ( );
			}
			m_BluetoothServersocket = tmpBluetoothServersocket;
			System.out.println ( "服务已开启" );
			
			// TODO 显示 “服务器线程已开启”
			transHandler.sendEmptyMessage ( 0x111 );
			
		}
		
		@ Override
		public void run ( ) {
			// TODO Auto-generated method stub
			super.run ( );
			// 阻塞监听直到异常出现或者有socket返回
			while ( requestAcceptThreadFlag ) {
				try {
					// TODO 显示 小图标 ：“服务器线程接受中·····”
					transHandler.sendEmptyMessage ( 0x112 );
					
					socketFromServer = m_BluetoothServersocket.accept ( );
					if ( socketFromServer != null ) {
						break;
					}
				} catch ( IOException e ) {
					System.out.println ( "exception while waiting" );
					break;
				}
			}
			
			System.out.println ( "接收到请求！" );
			
			if ( socketFromServer != null ) {
				System.out.println ( "连接已建立" );
				// TODO 显示 “服务器线程已接收到一个请求·”
				transHandler.sendEmptyMessage ( 0x113 );
			}
			
		}
		
		// 取消socket监听，关闭线程
		public void cancel ( ) {
			try {
				m_BluetoothServersocket.close ( );
			} catch ( IOException e ) {
			}
		}
	}
	
	public static BluetoothSocket socketFromClient = null;
	
	class RequestThread extends Thread {
		
		public RequestThread ( BluetoothDevice device ) {
			BluetoothSocket tmp = null;
			try {
				tmp = device.createRfcommSocketToServiceRecord ( S_UUID );
			} catch ( IOException e ) {
				System.out.println ( "RequestThread```tmpBluetoothServersocket```" );
				e.printStackTrace ( );
			}
			if ( tmp != null ) {
				socketFromClient = tmp;
			}
		}
		
		public void run ( ) {
			// TODO 显示 "连接中"
			transHandler.sendEmptyMessage ( 0x114 );
			while ( requestThreadFlag ) {// 必须多次连接以保证成功连接
				try {
					System.out.println ( "try to connection" );
					socketFromClient.connect ( );
					if ( socketFromClient.isConnected ( ) ) {
						// TODO 跳转
						transHandler.sendEmptyMessage ( 0x115 );
						break;
					}
					
				} catch ( IOException connectException ) {
					System.out.println ( "unable to connection" );
					// TODO 显示 "连接异常,请重连"
					transHandler.sendEmptyMessage ( 0x116 );
					try {
						socketFromClient.close ( );
					} catch ( IOException closeException ) {
					}
					break;
				}
			}
			System.out.println ( "连接成功，可发送数据" );
		}
		
		/** Will cancel an in-progress connection, and close the socket */
		public void cancel ( ) {
			try {
				socketFromClient.close ( );
			} catch ( IOException e ) {
			}
		}
	}
}

/**
adb uninstall vvnx.rpimon && \
adb install out/target/product/generic_arm64/system/app/RPiMon/RPiMon.apk && \
adb shell pm grant vvnx.rpimon android.permission.ACCESS_FINE_LOCATION
 * 
 * #stream servi en rtsp
 * test-launch "( rpicamsrc bitrate=2000000 keyframe-interval=15 ! video/x-h264,framerate=15/1,width=640,height=480 ! h264parse ! rtph264pay name=pay0 pt=96 )" &
 * 
 * #listen sur un port avec socat
 * socat TCP-LISTEN:4696,fork EXEC:/root/myscript.sh & 
 * 
 * #myscript.sh
 * #!/bin/sh

STDIN=$(cat) #Recuperation du message envoye par l emetteur

case "$STDIN" in
 start)
    kill `pidof test-launch`
    gst-launch-1.0 -e rpicamsrc bitrate=1000000 ! h264parse ! mp4mux ! filesink location=/root/capture.mp4
    ;;
 stop)
    kill -s SIGINT `pidof gst-launch-1.0`
    ;;  
 *)
esac

 * 
 * */

package vvnx.rpimon;

import android.app.Activity;
import android.os.Bundle;

import android.view.WindowManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.media.MediaPlayer;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.net.Uri;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.WpsInfo;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.IOException;

import android.util.Log;
import android.widget.Toast;



public class RPiMonActivity extends Activity implements PeerListListener {
		public static String TAG = "vvnx";   
		     
        private Button btn_1;
        public TextView txt_conn;
        
        private WifiP2pManager manager;
        private Channel channel;
        private WifiP2pConfig config;
        
        private SurfaceView surfaceView;
		private SurfaceHolder surfaceHolder;
		private MediaPlayer mediaPlayer;
		

		
		private final IntentFilter intentFilter = new IntentFilter();
		private BroadcastReceiver receiver = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = getLayoutInflater().inflate(R.layout.rpimon_activity, null);
        setContentView(view);
        
        btn_1 = findViewById(R.id.btn_1);
        txt_conn = findViewById(R.id.txt_conn);
        
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        
        mediaPlayer = new MediaPlayer();
		surfaceView = (SurfaceView)findViewById(R.id.video_surfaceview);
		final SurfaceHolder surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(new SurfCallBack());
		

        
    }
    
    
	//p2p.Wifi. On passe ici au launch, et à chaque réaffichage. je mets le discoverPeers ici
    @Override
    public void onResume() {
        super.onResume();
        receiver = new P2pBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
        
        Log.d(TAG, "onResume"); 
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                       
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        
                    }
                });
        
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
    
    

	@Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        //sans android.permission.ACCESS_FINE_LOCATION au runtime (pas manifest only), la WifiP2pDeviceList est vide
        //Log.d(TAG, "onPeersAvailable dans P2P_wifi activity et la liste = " + peerList.toString());//plusieurs infos pas seulement MACADDR
        
        /**
         * Au début je récupérais par mac addr, et il fallait que je vérifie si null ET le status:
         * WifiP2pDevice monPeerDevice = peerList.get("ba:27:eb:92:fc:8f"); //zero
         * if (monPeerDevice != null && monPeerDevice.status == 3) { //sans check du status je passe 200x/s ici (et donc dans manager.connect()) et ça fait planter connexion
         * 
         * Depuis mars 2022 je cherche le peer par deviceName dans la WifiP2pDeviceList que j'iterate avec getDeviceList().
         * Pour attribuer le name côté linux c'est: wpa_supplicant.conf: syntaxe: device_name=Zero
         * 
         * */
		
        for (WifiP2pDevice monPeerDevice : peerList.getDeviceList()) {			
			//Log.d(TAG, "name:" + monPeerDevice.deviceName);
			
			if (monPeerDevice.deviceName.equals("Zero")) {
			config = new WifiP2pConfig();
			config.deviceAddress = monPeerDevice.deviceAddress;
			config.wps.setup = WpsInfo.PBC;
				manager.connect(channel, config, new ActionListener() {
		            @Override
		            public void onSuccess() { //pas informatif sur le statut de la connexion
						//Log.d(TAG, "connect() --> onSuccess");
		                //Toast.makeText(P2P_wifi.this, "connect() --> onSuccess", Toast.LENGTH_SHORT).show();
		            }
		
		            @Override
		            public void onFailure(int reason) { //pas informatif sur le statut de la connexion
						//Log.d(TAG, "connect() --> onFailure, reason: " + reason);
						//Toast.makeText(P2P_wifi.this, "connect() --> onFailure, reason: " + reason, Toast.LENGTH_SHORT).show();
		            }
				});	
			}
		}  
    }
    
    public void rxGroupFormed() {
		//Log.d(TAG, "rxGroupFormed dans Main Activity");
		try {
		mediaPlayer.setDataSource(this, Uri.parse("rtsp://192.168.49.1:8554/test"));
		mediaPlayer.prepare();		
		mediaPlayer.start();		
		} catch (Exception e) {
		Log.e(TAG, "initialize Player Exception");
		e.printStackTrace();
		}
	}
    
    
    public void ActionPressBouton_1(View v) {
		sendMsg("start");	
	}
	
	public void ActionPressBouton_2(View v) {
		sendMsg("stop");	
	}
	
	
	//envoi message sur Socket
	public void sendMsg(String msg) {
		Log.d(TAG, "sendMsg:"+msg);
		//Thread sinon android.os.NetworkOnMainThreadException
		new Thread(new Runnable(){
		@Override
			public void run() {	        
				try {
			Socket socket = new Socket("192.168.49.1", 4696);
	        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
	        writer.println(msg);
	        socket.close(); //sinon accumulation de connexions peut poser pb socat (address already in use) mais aux tests mainly
				} catch (IOException e) {
                    Log.d(TAG, "erreur send socket :" + e.getMessage());
                } 
            }
		}).start();
	}
	
	
	private class SurfCallBack implements SurfaceHolder.Callback {
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      mediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }
  }
}


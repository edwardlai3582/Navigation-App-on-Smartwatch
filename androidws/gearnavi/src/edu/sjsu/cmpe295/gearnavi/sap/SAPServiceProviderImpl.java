/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.sap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.SA;
import com.samsung.android.sdk.accessory.SAAgent;
import com.samsung.android.sdk.accessory.SAPeerAgent;
import com.samsung.android.sdk.accessory.SASocket;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer;
import com.samsung.android.sdk.accessoryfiletransfer.SAFileTransfer.EventListener;
import com.squareup.otto.Subscribe;
//import com.yelp.example.Yelp;

import edu.sjsu.cmpe295.gearnavi.event.EventBusProvider;
import edu.sjsu.cmpe295.gearnavi.event.LocationChangedEvent;
import edu.sjsu.cmpe295.gearnavi.event.MapTileReadyEvent;
import edu.sjsu.cmpe295.gearnavi.service.LocationServiceImpl;
import edu.sjsu.cmpe295.gearnavi.service.MapServiceImpl;
import edu.sjsu.cmpe295.gearnavi.service.YelpServiceImpl;

/**
 * @author mly
 * 
 */
public class SAPServiceProviderImpl extends SAAgent {

    private static final String TAG = "SAPServiceProviderImpl";

    private static final int DATA_CHANNEL_ID = 123; // For generic data not tied to a service
    private static final int MAP_CHANNEL_ID = 222;
    private static final int POI_CHANNEL_ID = 444;
    private static final int ROT_CHANNEL_ID = 666;
    private static final int LOC_CHANNEL_ID = 888;


    
    private Location currentLocation;

    private Map<Integer, SAPServiceConnection> mConnectionPool;

    private final IBinder mBinder = new LocalBinder();

    private MapServiceImpl mMapService;

    private ServiceConnection mMapServiceConn = new ServiceConnection() {
        public void onServiceDisconnected(ComponentName className) {
            // TODO Auto-generated method stub
        }

        public void onServiceConnected(ComponentName className, IBinder service) {
            mMapService = ((MapServiceImpl.LocalBinder) service).getService();
        }
    };

    public SAPServiceProviderImpl() {
        super(TAG, SAPServiceConnection.class);
    }

    @Override
    public void onFindPeerAgentResponse(SAPeerAgent peerAgent, int result) {
        Log.d(TAG,
                "onFindPeerAgentResponse | " + "AppName:"
                        + peerAgent.getAppName() + ", " + "DeviceName"
                        + peerAgent.getDeviceName() + ", " + "PeerId"
                        + peerAgent.getPeerId() + ", " + "Result" + result);
    }

    @SuppressLint("UseSparseArrays")
    @Override
    public void onServiceConnectionResponse(SASocket socket, int result) {
        if (result == SAAgent.CONNECTION_SUCCESS) {
            if (socket != null) {
                if (mConnectionPool == null) {
                    mConnectionPool = new HashMap<Integer, SAPServiceProviderImpl.SAPServiceConnection>();
                }

                SAPServiceConnection conn = (SAPServiceConnection) socket;
                conn.mConnectionId = (int) (System.currentTimeMillis() & 255);
                mConnectionPool.put(conn.mConnectionId, conn);

                Log.d(TAG,
                        "onServiceConnectionResponse | Connection success (id:"
                                + conn.mConnectionId + ")");
            }
        }

        // Register this class to listen for events on bus at this time
        EventBusProvider.getInstance().register(SAPServiceProviderImpl.this);

        // Download initial maps
        mMapService.fetchOsmTiles(currentLocation, 0);
    }

    /**
     * Thread created for sending data
     */
    private void sendData(final int connectionId, final int channelId,
            final byte[] data) {

        final SAPServiceConnection socket = mConnectionPool.get(connectionId);

        if (socket == null) {
            Log.e(TAG,
                    "Error getting SAPServiceConnection object. Connection Id: "
                            + connectionId);
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    socket.send(channelId, data);
                } catch (IOException e) {
                    Log.e(TAG, "IOExcepption occur on sendData");
                }
            }
        }).start();
    }

    /**
     * Thread created for sending file
     */
    private void sendFile(final int connectionId, final String filename) {

        // Listener for file transfer status callback
        EventListener fileTransferSenderCallback = new EventListener() {

            public void onProgressChanged(int transactionId, int progress) {
                // TODO Auto-generated method stub
            }

            public void onTransferCompleted(int transactionId, String fileName,
                    int errorCode) {
                Log.d(TAG, "onTransferCompleted" + " | transaction id: "
                        + transactionId + " | filename: " + fileName
                        + " | errorCode: " + errorCode);
            }

            public void onTransferRequested(int transactionId, String fileName) {
                // TODO Auto-generated method stub
            }
        };

        final SAFileTransfer fileTransfer = new SAFileTransfer(
                SAPServiceProviderImpl.this, fileTransferSenderCallback);

        final SAPeerAgent peerAgent = mConnectionPool.get(connectionId)
                .getConnectedPeerAgent();
        if (peerAgent == null) {
            Log.e(TAG,
                    "sendFile | Error retrieving connected peer agent. Connection id: "
                            + connectionId);
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                fileTransfer.send(peerAgent, filename);
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public SAPServiceProviderImpl getService() {
            return SAPServiceProviderImpl.this;
        }
    }

    public class SAPServiceConnection extends SASocket {
        private int mConnectionId;

        public SAPServiceConnection() {
            super(SAPServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
            Log.e(TAG, "SASocket error | " + errorMessage + " : Error code "
                    + errorCode);
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            Log.d(TAG, " ... onReceive ... ");
            onDataAvailableOnChannel(channelId, new String(data));
        }

        @Override
        public void onServiceConnectionLost(int reason) {
            Log.e(TAG, "onServiceConnectionLost | Reason code: " + reason);

            if (mConnectionPool != null) {
                mConnectionPool.remove(mConnectionId);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        SA accessory = new SA();
        try {
            accessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Start or bind to other services
        startService(new Intent(this, LocationServiceImpl.class));

        bindService(new Intent(SAPServiceProviderImpl.this,
                MapServiceImpl.class), mMapServiceConn,
                Context.BIND_AUTO_CREATE);
    }

    private void onDataAvailableOnChannel(int channelId, String data) {
    	if (channelId == MAP_CHANNEL_ID) {
    		// Retrieve map tile from service
    		
    		JSONObject locData;
    		double latitude = 0.0;
    		double longitude = 0.0;
			try {
				locData = new JSONObject(data);
				latitude = locData.getDouble("lat");
	    		longitude = locData.getDouble("lon");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		mMapService.fetchOsmTiles(latitude, longitude, 16);
    		
    	} else if (channelId == POI_CHANNEL_ID) {
    		// Retrieve poi data from service	
    		
    		YelpServiceImpl mYelp = new YelpServiceImpl();
    		String jsonlist=mYelp.namelist(data, Double.toString(currentLocation.getLatitude())+","+Double.toString(currentLocation.getLongitude()));
    		sendData(mConnectionPool.entrySet().iterator().next().getKey(), POI_CHANNEL_ID, jsonlist.getBytes());
    	
    	} else if (channelId == ROT_CHANNEL_ID) {
    		// Retrieve routing data from service
    	
    		String readJSONFeed = getDirection(data);
    		sendData(mConnectionPool.entrySet().iterator().next().getKey(), ROT_CHANNEL_ID, readJSONFeed.getBytes());
        	
    		
    	} else {
    		Log.e(TAG, "... Incoming data from channel id " + channelId + " not recognized ");
    	}
    }

    @Subscribe
    public void locationChangedSubscriber(LocationChangedEvent event) {

        currentLocation = event.location;

        if(currentLocation.getProvider().equals("NA".intern())) {
        	// Do nothing
        	return;
        }
        
        // Generate message in json
        JSONObject locationData = new JSONObject();
        try {
            locationData.put("lon", currentLocation.getLongitude());
            locationData.put("lat", currentLocation.getLatitude());
        } catch (JSONException e) {
            Log.e(TAG, " JSONExceptino thrown in locationChangedSubscriber");
        }

        Log.d(TAG, " ... Sending updated location " + event.location.toString());

        sendData(mConnectionPool.entrySet().iterator().next().getKey(),
                LOC_CHANNEL_ID, locationData.toString().getBytes());
    }

    @Subscribe
    public void mapTileReadySubscriber(MapTileReadyEvent event) {

        String fullPath = new String(Environment.getExternalStorageDirectory()
                .toString()
                + File.separator
                + Environment.DIRECTORY_DOWNLOADS
                + File.separator + event.filename);

        Log.d(TAG, " ... Transfering tile " + event.filename);

        sendFile(mConnectionPool.entrySet().iterator().next().getKey(),
                fullPath);
    }
    

	
	public String getDirection(String add) {
		String destlatt=null,destlongg=null;
		try {
		  Geocoder coder = new Geocoder(this);
          List<Address> addressList = coder.getFromLocationName(add, 1);
          if (addressList != null && addressList.size() > 0) {
            double lat = addressList.get(0).getLatitude();
            destlatt =String.valueOf(lat);
            double lng = addressList.get(0).getLongitude();
            destlongg =String.valueOf(lng);
        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }    
		StringBuilder builder = new StringBuilder();
		HttpClient client = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet(
				"http://maps.googleapis.com/maps/api/directions/json?origin="+Double.toString(currentLocation.getLatitude())+","+Double.toString(currentLocation.getLongitude())+"&destination="+destlatt+","+destlongg+"&sensor=false&mode=walking");
		try {
			HttpResponse response = client.execute(httpGet);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
			} else {
				//Log.e(MainActivity.class.toString(), "Failed to download file");
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
}
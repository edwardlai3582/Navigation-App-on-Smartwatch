/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * @author layya_000
 *
 */
public class GHDirectionsServiceImpl extends Service {

	/* (non-Javadoc)
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	//Subscribe for LocationChangedEvent
	//Check proximity
	//If within proximity, retrieve next node
	//Update with next-node lat lon in ProximityAlert
	//Register BroadcastReceiever
	//Post to ProximityEvent
}

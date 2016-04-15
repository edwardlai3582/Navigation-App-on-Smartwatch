/**
 * 
 */
package edu.sjsu.cmpe295.gearnavi.service;

import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import edu.sjsu.cmpe295.gearnavi.event.EventBusProvider;
import edu.sjsu.cmpe295.gearnavi.event.MapTileReadyEvent;
import edu.sjsu.cmpe295.gearnavi.model.OsmTileModel;

/**
 * @author mly
 * 
 */
public class MapServiceImpl extends Service {

    private static final String MAP_SCHEMA = "http";
    private static final String MAP_AUTHORITY = "tile.openstreetmap.org";
    private static final int DEFAULT_DEPTH = 2;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MapServiceImpl getService() {
            return MapServiceImpl.this;
        }
    }

    @SuppressLint("UseSparseArrays")
    private Map<Long, String> mActivatedDownloads = new HashMap<Long, String>();

    private BroadcastReceiver downloadCompleteReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle extras = intent.getExtras();
            long downloadId = extras.getLong(DownloadManager.EXTRA_DOWNLOAD_ID);

            String filename = mActivatedDownloads.get(downloadId);

            EventBusProvider.getInstance()
                    .post(new MapTileReadyEvent(filename));
        }
    };

    /**
     * Queue download for map tiles by provided location
     * 
     * @param location
     * @param zoomLevel
     */
    public void fetchOsmTiles(Location location, int zoomLevel) {
        fetchOsmTiles(location.getLatitude(), location.getLongitude(),
                zoomLevel);
    }

    public void fetchOsmTiles(double latitude, double longitude, int zoomLevel) {
        fetchOSMTiles(locationToOsmTile(latitude, longitude, zoomLevel));
    }

    public void fetchOSMTiles(OsmTileModel targetOsmTile) {

        // Fetch target OSM tile and surrounding tiles. The depth determines how
        // many layers around the target tile to fetch.
        for (int i = 0; i < DEFAULT_DEPTH; i++) {
            for (int x = 0 - i; x <= i; x++) {
                for (int y = 0 - i; y <= i; y++) {
                    if (Math.abs(x) == i || Math.abs(y) == i) {

                        fetchTile(new OsmTileModel(targetOsmTile.getX() + x,
                                targetOsmTile.getY() + y,
                                targetOsmTile.getZoom()));
                        
                    }
                }
            }

        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Register receiver to be notified of completed downloads
        registerReceiver(
                downloadCompleteReceiver, 
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        EventBusProvider.getInstance().register(this);
    }

    /**
     * Convert location information to an osm tile model
     * 
     * @param latitude
     * @param longitude
     * @param zoomLevel
     * @return
     */
    private OsmTileModel locationToOsmTile(double latitude, double longitude,
            int zoomLevel) {

        OsmTileModel osmTile = new OsmTileModel();

        osmTile.setZoom(zoomLevel);

        double n = Math.pow(2, osmTile.getZoom());
        osmTile.setX((int) (n * ((longitude + 180) / 360)));

        double latitudeInRadian = Math.toRadians(latitude);
        osmTile.setY((int) ((1.0 - Math.log(Math.tan(latitudeInRadian)
                + (1 / Math.cos(latitudeInRadian)))
                / Math.PI) / 2.0 * n));

        return osmTile;
    }

    /**
     * Queue target map tile request if tile has not been downloaded.
     * 
     * @param path
     *            Osm filename
     * @param targetPath
     *            path & filename to save download as
     * @return download id produce by DownloadManager for request
     */
    private void fetchTile(OsmTileModel targetOsmTile) {

        // Check if file already downloaded
        if (!mActivatedDownloads.containsValue(targetOsmTile.toFilename()
                .intern())) {

            // Build the uri
            Uri uri = new Uri.Builder().scheme(MAP_SCHEMA)
                    .authority(MAP_AUTHORITY)
                    .path(targetOsmTile.toApiPathName()).build();

            // Build request
            DownloadManager.Request request = new DownloadManager.Request(uri)
                    .setVisibleInDownloadsUi(false)
                    .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_HIDDEN)
                    .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            targetOsmTile.toFilename());

            // Queue request for download
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);

            // store record in memory
            mActivatedDownloads.put(downloadId, targetOsmTile.toFilename()
                    .intern());
        }
    }
}
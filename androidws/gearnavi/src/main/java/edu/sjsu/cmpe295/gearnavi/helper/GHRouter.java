package edu.sjsu.cmpe295.gearnavi.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mapsforge.core.model.LatLong;

import android.graphics.Path;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.util.Constants;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.StopWatch;

public class GHRouter {
    private LatLong start;
    private LatLong end;
    private String currentArea = "california-gh";
    private File mapsFolder;
    private GraphHopper hopper;
    private volatile boolean prepareInProgress = false;
    private volatile boolean shortestPathRunning = false;
    //    private PointList il;
    private InstructionList il;
    //    private List<Double[]> iList;
    private List<String> iList;
    private ArrayList<String> gList;
    private List < Map <String,Object> > ilJSON;
    
	public GHRouter() {
		super();

        boolean greaterOrEqKitkat = Build.VERSION.SDK_INT >= 19;
        if (greaterOrEqKitkat)
        {
            if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                log("GraphHopper is not usable without an external storage!");
                return;
            }
            mapsFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "/graphhopper/maps/");
        } else
            mapsFolder = new File(Environment.getExternalStorageDirectory(), "/graphhopper/maps/");
        initFiles();
	}
	
    public void calcPath( final double fromLat, final double fromLon,
	            final double toLat, final double toLon )
	{
	
		log("calculating path ...");
		  GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
		          setAlgorithm("dijkstrabi").
		          putHint("instructions", true).
		          putHint("douglas.minprecision", 1);
		  GHResponse resp = hopper.route(req);
		  if (!resp.hasErrors())
		  {
		
			  InstructionList ilTemp = resp.getInstructions();
			  setIl(ilTemp);
		      int count = il.getSize();
		      iList = il.createDistances(true);
		      ilJSON = il.createJson();
		      log("from:" + fromLat + "," + fromLon + " to:" + toLat + ","
		              + toLon + " found path with distance:" + resp.getDistance()
		              / 1000f + ", nodes:" + resp.getPoints().getSize() + " " + resp.getDebugInfo());
		
		  } else
		  {
		      log("Error:" + resp.getErrors());
		  }
		  shortestPathRunning = false;
	}

    private void initFiles()
    {
        prepareInProgress = true;
        loadGraphStorage();
    }

    void loadGraphStorage()
    {
        log("loading graph (" + Constants.VERSION + ") ... ");
        new GHAsyncTask<Void, Void, Path>()
        {
            protected Path saveDoInBackground( Void... v ) throws Exception
            {
                GraphHopper tmpHopp = new GraphHopper().forMobile();
                tmpHopp.setCHShortcuts("fastest");
                tmpHopp.load(new File(mapsFolder, currentArea).getAbsolutePath());
                //tmpHopp.setInMemory(true);
                //tmpHopp.setEncodingManager(new EncodingManager("car"));
                log("found graph " + tmpHopp.getGraph().toString() + ", nodes:" + tmpHopp.getGraph().getNodes());
                hopper = tmpHopp;
                return null;
            }

            protected void onPostExecute( Path o )
            {
                if (hasError())
                {
                    log("An error happend while creating graph:"
                            + getErrorMessage());
                } else
                {
                    log("Finished loading graph. Enter where to start and end the route.");
                }

                finishPrepare();
            }
        }.execute();
    }

    private void finishPrepare()
    {
        prepareInProgress = false;
    }


//    private void logUser( String str )
//    {
//        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
//    }

    private void log( String str )
    {
        Log.i("GH", str);
    }

	public LatLong getStart() {
		return start;
	}
	public void setStart(LatLong start) {
		this.start = start;
	}
	public LatLong getEnd() {
		return end;
	}
	public void setEnd(LatLong end) {
		this.end = end;
	}
	public String getCurrentArea() {
		return currentArea;
	}
	public void setCurrentArea(String currentArea) {
		this.currentArea = currentArea;
	}
	public File getMapsFolder() {
		return mapsFolder;
	}
	public void setMapsFolder(File mapsFolder) {
		this.mapsFolder = mapsFolder;
	}
	public GraphHopper getHopper() {
		return hopper;
	}
	public void setHopper(GraphHopper hopper) {
		this.hopper = hopper;
	}
	public boolean isPrepareInProgress() {
		return prepareInProgress;
	}
	public void setPrepareInProgress(boolean prepareInProgress) {
		this.prepareInProgress = prepareInProgress;
	}
	public boolean isShortestPathRunning() {
		return shortestPathRunning;
	}
	public void setShortestPathRunning(boolean shortestPathRunning) {
		this.shortestPathRunning = shortestPathRunning;
	}
	public InstructionList getIl() {
		return il;
	}
	public void setIl(InstructionList il) {
		this.il = il;
	}
	public List<String> getiList() {
		return iList;
	}
	public void setiList(List<String> iList) {
		this.iList = iList;
	}
	public ArrayList<String> getgList() {
		return gList;
	}
	public void setgList(ArrayList<String> gList) {
		this.gList = gList;
	}

	public List<Map<String, Object>> getIlJSON() {
		return ilJSON;
	}

	public void setIlJSON(List<Map<String, Object>> ilJSON) {
		this.ilJSON = ilJSON;
	}


}

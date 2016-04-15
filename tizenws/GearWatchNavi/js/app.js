/** ************************************************************************** */

var PROVIDER_APP_NAME = "gearnavi";
var DATA_CHANNEL_ID = 123; // For generic data not tied to a service
var MAP_CHANNEL_ID = 222;
var POI_CHANNEL_ID = 444;
var ROT_CHANNEL_ID = 666;
var LOC_CHANNEL_ID = 888;
var FILE_STORAGE_PATH = "file:///opt/usr/media/Downloads/";

var SAAgent = null;
var SASocket = null;
var SAFileTransfer = null;

var currentLocationJson = null;

var rr = [];

var map = null;
var polylines= null;
var destIcon = L.icon({iconUrl: 'js/images/marker-icon-purple.png', iconAnchor:[12, 41],});
var destMarker;
var mapControl = null;
var layer = null;
var currMapCenter = null;
var userPosMarker = null;
// var userPosIcon = L.icon( { iconUrl:
// 'file:///opt/usr/apps/YuW4qTtJdB/res/wgt/res/images/marker-icon.png' });;
var destPosMarker = null;
// var destPosIcon = null;

var isConnected = false;

var receiveFileTransferCallback = {
	onreceive : function(transferId, fileName) {
		console
				.log(" ... Incoming file transfer request from connected peer agent: "
						+ transferId + " | " + fileName);
		SAFileTransfer.receiveFile(transferId, FILE_STORAGE_PATH + fileName);
	},
	onprogress : function(transferId, progress) {
		console.log("... File transfer in progress " + transferId + " "
				+ progress);
	},
	oncomplete : function(transferId, localPath) {
		console.log("File transfer completed: " + transferId + " | "
				+ localPath);

		// add redraw method here
		layer.redraw();
	},
	onerror : function(errorCode, transferId) {
		console.log("File transfer failure detected TxId:" + transferId
				+ " ErrCode:" + errorCode);
	}
};

/** ************************************************************************** */
/*
 * Error logging to console
 */
function onerror(err, tag) {
	console.log(tag + "err [" + err.name + "] msg[" + err.message + "]");
}

/*
 * Responsible for connection to the android side
 */
var agentCallback = {
	onconnect : function(socket) {
		isConnected = true;
		SASocket = socket;
		SASocket.setSocketStatusListener(function(reason) {
			console.log("Service connection lost, Reason : [" + reason + "]");
			disconnect();
		});

		// Begin listening for data and file transfers
		SASocket.setDataReceiveListener(onreceive);
		SAFileTransfer = SAAgent.getSAFileTransfer();
		SAFileTransfer.setFileReceiveListener(receiveFileTransferCallback);
	},
	onerror : onerror
}

/*
 * Locates peer using SAP by provider name
 */
var peerAgentFindCallback = {
	onpeeragentfound : function(peerAgent) {
		try {
			if (peerAgent.appName === PROVIDER_APP_NAME) {
				SAAgent.setServiceConnectionListener(agentCallback);
				SAAgent.requestServiceConnection(peerAgent);
			} else {
				alert("Not expected app!! : " + peerAgent.appName);
			}
		} catch (err) {
			onerror(err, "1 ");
		}
	},
	onerror : onerror
};

/*
 * Handle successful connection
 */
function onsuccess(agents) {
	try {
		if (agents.length > 0) {
			SAAgent = agents[0];

			SAAgent.setPeerAgentFindListener(peerAgentFindCallback);
			SAAgent.findPeerAgents();
		} else {
			alert("Not found SAAgent!!");
		}
	} catch (err) {
		onerror(err, "2 ");
	}
}

/*
 * Connect action
 */
function connect() {
	if (SASocket) {
		alert('Already connected!');
		return false;
	}
	try {
		webapis.sa.requestSAAgent(onsuccess, onerror);
	} catch (err) {
		onerror(err, "3 ");
	}
}

/*
 * Disconnect action
 */
function disconnect() {
	try {
		if (SASocket !== null) {
			SASocket.close();
			SASocket = null;
		}
	} catch (err) {
		onerror(err, "4 ");
	} finally {
		isConnected = false;
	}
}

/*
 * Handle onreceive
 */
function onreceive(channelId, data) {
	console.log(" ... Incoming data on channel " + channelId + " " + data);

	if (channelId == LOC_CHANNEL_ID) {
		currentLocationJson = JSON.parse(data);
		var locationPoint = [ currentLocationJson.lat, currentLocationJson.lon ];

		if (map === null) {
			initMap();

			// update current location marker here ... below did not work for
			// some reason

			userPosMarker = L.marker(locationPoint
			// , userPosIcon
			).addTo(map);
			
			// Custom button using Leaflet.EasyButton plugin
			L.easyButton(
					'fa-comment', 
					function() {
						map.panTo([ currentLocationJson.lat, currentLocationJson.lon ]);
					}, 
					'');
		} else {
			// update current location marker
			userPosMarker.setLatLng(locationPoint).update();
		}

	}
	
	else if (channelId == POI_CHANNEL_ID){
			//
			$( "#process" ).remove();
			//
			var yelpJSON = JSON.parse ( data );
			for (var i = 0; i < yelpJSON.businesses.length; i++) { 

				var wholeadd=yelpJSON.businesses[i].location.address+","+yelpJSON.businesses[i].location.city+","+yelpJSON.businesses[i].location.state_code+" "+yelpJSON.businesses[i].location.postal_code;
				console.log(wholeadd);
				var pattern =/(stars_small)(.)*(png)/ ;
				var starpic =yelpJSON.businesses[i].rating_img_url_small;
				var starn =starpic.match(pattern);
				console.log(starn[0]);
				var distancea =yelpJSON.businesses[i].distance.toFixed(0);
				var disabled =yelpJSON.businesses[i].is_closed?"disabled":"";
				var closed   =yelpJSON.businesses[i].is_closed?"closed":"";
				//console.log(disabled);
				//console.log(closed);
				$('#popop ul').append('<li class="li-has-multiline-sup'+disabled+'"><a href="#" onclick="fetchgoogle(\''+wholeadd+'\');$(\'#popop ul\').empty();">'+yelpJSON.businesses[i].name+'<span class="li-text-sub"><img src="js/images/'+starn[0]+'" >  dist:'+distancea+'m '+closed+'</span></a></li>');
				
			}
	}
	///*
	else if(channelId == ROT_CHANNEL_ID){
		var hehe = JSON.parse ( data );
		  //var log = document.getElementById('hasdirections');
		  //log.innerHTML = "";
		  ///////////////////	
		  rr = [];
		  ///////////////////
		  if (Object.keys(hehe)[0]==="routes"){
			  
			  $('#hasdirections').empty();
			  var divid = document.getElementById("hasdirections");
			  divid.scrollTop = 0;
			  
			  console.log("aaaaa");  
			for (i = 0; i < hehe.routes[0].legs[0].steps.length; i++) { 
			     // createHTML(hehe.routes[0].legs[0].steps[i].html_instructions);
			     // createHTML(hehe.routes[0].legs[0].steps[i].distance.text);
				rr.push({ html_instructions: '', distance: '' });
				rr[i].end_location_lat     =hehe.routes[0].legs[0].steps[i].end_location.lat;
				rr[i].end_location_lng     =hehe.routes[0].legs[0].steps[i].end_location.lng;
				rr[i].html_instructions    =hehe.routes[0].legs[0].steps[i].html_instructions;
				//rr[i].distance         =hehe.routes[0].legs[0].steps[i].distance.text;
				rr[i].distance         =hehe.routes[0].legs[0].steps[i].distance.value;
		    }
			
			var overviewpolyline=hehe.routes[0].overview_polyline.points;
			console.log("poly:"+overviewpolyline); 
			
			if(polylines==null){
			  var encoded = overviewpolyline;
			  polylines = L.Polyline.fromEncoded(encoded,{weight: 10,color: '#0000FF'}).addTo(map);
			  destMarker=L.marker([rr[rr.length-1].end_location_lat, rr[rr.length-1].end_location_lng], {icon: destIcon}); 
			  map.addLayer(destMarker);
		    } 
		    else{
		    	map.removeLayer(polylines);	
		    	map.removeLayer(destMarker);
		    	var encoded = overviewpolyline;
				polylines = L.Polyline.fromEncoded(encoded,{weight: 10,color: '#0000FF'}).addTo(map);
				destMarker=L.marker([rr[rr.length-1].end_location_lat, rr[rr.length-1].end_location_lng], {icon: destIcon}); 
				map.addLayer(destMarker);
		    }
			//var polygon = L.Polygon.fromEncoded(encoded, {
			//	weight: 1,
			//	color: '#f30'
			//}).addTo(map);
			
			
			showstep();
		  }
	}
	//*/
}

/*
 * Initiate leaflet map
 */
function initMap() {
	map = L.map('map', {
		zoomControl : false
	});

	map.setView([ currentLocationJson.lat, currentLocationJson.lon ], 16);

	map.on('moveend', function(event) {
		if (currMapCenter !== map.getCenter()) {
			currMapCenter = map.getCenter();
			console.log(" ... New center detected : " + currMapCenter);
			doTileRqst(currMapCenter);
		}
	});

	// Add map tile layer
	layer = L.tileLayer('file:///opt/usr/media/Downloads/16_{x}_{y}.png', {
		minZoom : 16,
		maxZoom : 16
	}).addTo(map);

	// Add controls
	// mapControl = L.control(
	// ).addTo(map);
}

function doTileRqst(latlng) {
	try {
		var msg = '{"lat":' + latlng.lat + ', "lon":' + latlng.lng + ' }';
		SASocket.sendData(MAP_CHANNEL_ID, msg);
	} catch (err) {
		onerror(err, "5 ");
	}
}

///
function creatyelplist(foodtype){
	
	$('#popop').append('<div class="ui-processing" id="process"></div>');
	fetchyelp(foodtype);
	
	
}

function fetchyelp(loco) {
	//LOCATION=loco;
	try {
		//SASocket.setDataReceiveListener(onreceive);
		SASocket.sendData(POI_CHANNEL_ID, loco);
	} catch(err) {
		console.log("exception [" + err.name + "] msg[" + err.message + "]");
	}
}

function fetchgoogle(loco) {
	//$('#sections').append('<section id="directionpage"><div class="ui-content" id="hasdirections"></div></section>');
	try {
		//SASocket.setDataReceiveListener(onreceive);
		SASocket.sendData(ROT_CHANNEL_ID, loco);
	} catch(err) {
		console.log("exception [" + err.name + "] msg[" + err.message + "]");
	}
}

function showstep(){
	$('#hasdirections').append('<section id="instruction" style="border:2px #660066 solid;border-radius:10px;background-color:#CC0099;"></section>');
	$('#hasdirections').append('<section id="distance"    style="border:2px #660066 solid;border-radius:10px;background-color:#FF0066;"></section>');

	/*  
	for(var i=0;i<rr.length;i++){
	  $('#instruction').html(rr[i].html_instructions);
	  $('#distance').html(rr[i].distance);
	  console.log(rr[i].html_instructions);
	  while(getDistanceFromLatLonInKm(currentLocationJson.lat,currentLocationJson.lon,rr[i].end_location_lat,rr[i].end_location_lng)>1){
			var dis=currentLocationJson.lat,currentLocationJson.lon,rr[i].end_location_lat,rr[i].end_location_lng);
			$('#distance').html(dis);
			console.log(rr[i].distance);
	  }  
	  navigator.vibrate(2000);
	} 
	$('#instruction').html("arrived!"); 
	*/
	///*
	setTimeout(function(){
		$('#instruction').html(rr[0].html_instructions);
		$('#distance').html(rr[0].distance);
	}, 2000);
	setTimeout(function(){
		navigator.vibrate(2000);
		$('#instruction').html(rr[1].html_instructions);
		$('#distance').html(rr[1].distance);
	}, 4000);
	setTimeout(function(){
		navigator.vibrate(2000);
		$('#instruction').html(rr[2].html_instructions);
		$('#distance').html(rr[2].distance);
	}, 6000);
	setTimeout(function(){
		navigator.vibrate(2000);
		$('#instruction').html(rr[3].html_instructions);
		$('#distance').html(rr[3].distance);
	}, 8000);
	//*/
}

function getDistanceFromLatLonInKm(lat1,lon1,lat2,lon2) {
	  var R = 6371; // Radius of the earth in km
	  var dLat = deg2rad(lat2-lat1);  // deg2rad below
	  var dLon = deg2rad(lon2-lon1); 
	  var a = 
	    Math.sin(dLat/2) * Math.sin(dLat/2) +
	    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * 
	    Math.sin(dLon/2) * Math.sin(dLon/2)
	    ; 
	  var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
	  var d = R * c*1000; // Distance in m
	  return d;
}

function deg2rad(deg) {
	  return deg * (Math.PI/180)
}

/** ************************************************************************** */

(function() {
	window
			.addEventListener(
					'tizenhwkey',
					function(ev) {
						if (ev.keyName == "back") {
							var page = document
									.getElementsByClassName('ui-page-active')[0], pageid = page ? page.id
									: "";
							if (pageid === "main") {
								tizen.application.getCurrentApplication()
										.exit();
							} else {
								window.history.back();
							}
						}
					});
}());

connect();
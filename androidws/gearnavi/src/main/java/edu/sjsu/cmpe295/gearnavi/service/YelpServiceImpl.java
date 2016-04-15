package edu.sjsu.cmpe295.gearnavi.service;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.scribe.model.Token;
import org.scribe.builder.api.DefaultApi10a;
//import com.yelp.example.YelpApi2;

public class YelpServiceImpl extends DefaultApi10a{
	  OAuthService service;
	  Token accessToken;

		private static final String CONSUMER_KEY = "V6BLNYVnhJuekBq-dr3H3w";
		private static final String CONSUMER_SECRET = "6L0OInhhEhQmfW-a_sVwF7OLXnE";
		private static final String TOKEN = "WINw_DBk20USK6rgdqDLPLadiGpJ-n5F";
		private static final String TOKEN_SECRET = "lWudOKU0i0LYCMfnzbne7TyzVJM";
	  
	  public YelpServiceImpl() {
	    this.service = new ServiceBuilder().provider(this).apiKey(CONSUMER_KEY).apiSecret(CONSUMER_SECRET).build();
	    this.accessToken = new Token(TOKEN, TOKEN_SECRET);
	  }


	  public String search(String term, String location) {
	    OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
	    request.addQuerystringParameter("term", term);
	    request.addQuerystringParameter("location", location);
	    request.addQuerystringParameter("limit", "5");
	    this.service.signRequest(this.accessToken, request);
	    Response response = request.send();
	    return response.getBody();
	  }
	  
	  public String namelist(String term, String location) {
		    OAuthRequest request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
		    request.addQuerystringParameter("term", term);
		    //request.addQuerystringParameter("location", location);
		    request.addQuerystringParameter("ll", location);
		    request.addQuerystringParameter("limit", "10");
		    request.addQuerystringParameter("sort", "1");
		    this.service.signRequest(this.accessToken, request);
		    Response response = request.send();
		    String searchResponseJSON=response.getBody();

		    return searchResponseJSON;
	}
	  
	  @Override
	  public String getAccessTokenEndpoint() {
	    return null;
	  }

	  @Override
	  public String getAuthorizationUrl(Token arg0) {
	    return null;
	  }

	  @Override
	  public String getRequestTokenEndpoint() {
	    return null;
	  }

}



package people.client;

import java.util.Date;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Random;


/**
 * http://www.google.com/reader/view/#stream/feed%2Fhttp%3A%2F%2Fwww.eishay.com%2Ffeeds%2Fposts%2Fdefault
 * http://gwt-widget.sourceforge.net/docs/apidocs/org/gwtwidgets/client/util/CookieUtils.html
 * http://gwt-widget.sourceforge.net/docs/apidocs/org/gwtwidgets/client/util/Cookie.html
 * 
 * Cookies for keeping track of sessions.
 * 
 */
public class SessionManager  {
	
	/*
	 * I don't know what I mean by a "session" yet. !@#$
	 */
	private String sessionId = null;
	
	public void startSession() {
		if (this.sessionId == null) {
			this.sessionId = Cookies.getCookie("SESSION_ID");
		    if (this.sessionId == null) {
		    	this.sessionId  = "SESSION_ID" 
		    		+ "." + System.currentTimeMillis() 
		    		+ "." + Random.nextInt() 
		    		+ "." + Random.nextDouble();
		    	Cookies.setCookie("SESSION_ID", this.sessionId );
		    }
		}
	}
	
	private static final int SIZE_RECENT_UNIQUE_IDS = 10;
	private static final String  NAME_RECENT_UNIQUE_IDS = "ids";
	
	public long[] getRecentUniqueIDs(int limit) {
		long[] recentUniqueIDs = null;
		String[] values;
		String recentUniqueIDsValue = Cookies.getCookie(NAME_RECENT_UNIQUE_IDS);
		if (recentUniqueIDsValue != null) {
			values = recentUniqueIDsValue.split(",", limit);
			if (values != null) {
				long[] tmp = new long[values.length];
				int numValues = 0;
				for (int i = 0; i < values.length; ++i) {
					tmp[i] = Long.parseLong(values[i]);
					if (tmp[i] > 0)
						++numValues;
				}
				if (numValues > 0)	{
					recentUniqueIDs = new long[numValues];
					int j = 0;
					for (int i = 0; i < values.length; ++i) {
						if (tmp[i] > 0)
							recentUniqueIDs[j++] = tmp[i];
					}
				}
			}
		}
		return recentUniqueIDs;
	}
	
	public void setRecentUniqueIDs(long[] recentUniqueIDs){
		String recentUniqueIDsValue = "";
		if (recentUniqueIDs != null) {
			for (int i = 0; i < recentUniqueIDs.length && i < SIZE_RECENT_UNIQUE_IDS; ++i) {
				recentUniqueIDsValue += "" + recentUniqueIDs[i] + ",";
			}
			Date date = new Date();
			final long YEAR = 31*1000*1000*1000; 
			date.setTime(System.currentTimeMillis() +YEAR );
			Cookies.setCookie(NAME_RECENT_UNIQUE_IDS, recentUniqueIDsValue, date );
		}
	}
}
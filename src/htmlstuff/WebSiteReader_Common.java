package htmlstuff;

import htmlstuff.WebSiteReader_Quota.ActionType;

import java.util.Calendar;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.htmlparser.Parser;
import org.htmlparser.http.ConnectionManager;
import org.htmlparser.http.Cookie;
import org.htmlparser.util.ParserException;


public class WebSiteReader_Common {
	  private static final Logger log = Logger.getLogger(WebSiteReader_Common.class.getName());
	 
	private static WebSiteReader_Common instance = null;
	protected WebSiteReader_Common() {   }  // Exists only to defeat instantiation.
	public static WebSiteReader_Common getInstance() {
	    if (instance == null) {
	         instance = new WebSiteReader_Common();
	    }
	    return instance;
	}
	
	/* http://htmlparser.sourceforge.net/javadoc/index.html
	 * public static ConnectionManager getConnectionManager()
		Get the connection manager all Parsers use.
		Returns: The connection manager.
		See Also: setConnectionManager(org.htmlparser.http.ConnectionManager)
	 */
	
	private static void setACooky(ConnectionManager manager, String url, String domain, 
			String key, String value) {
		Cookie cookie = new Cookie (key, value);
		cookie.setDomain (domain);
		manager.setCookie (cookie, null); // url not needed if domain specified?
	//	manager.setCookie (cookie, url);
	}
	/* bcookie 			d2375dea-b963-49e4-90f9-71a30eeccd25 www.linkedin.com
	 * srchId  			649ee890-b59e-472c-bb88-86301185d410
	 * leo_auth_token  LIM:1960788:i:1244080991:77a7f084f010a8f668d6f226805e34ef2d6eb60a
	 * NSC_MC_WT_DTQ_IUUQ  e2420fbd199f
	 * NSC_MC_QH_MFP       e2420d8429a0
	 */
	
	private  boolean _cookiesHaveBeenSet = false;
	private void reset() {
		_cookiesHaveBeenSet = false;
	}
	private void setupCookies(boolean isForced) 
		throws ParserException {
		if (!_cookiesHaveBeenSet || isForced) {
			_cookiesHaveBeenSet = true;
			ConnectionManager manager = Parser.getConnectionManager ();
			log.log(Level.INFO, "setupCookies()");
			// set up cookies
			String url = "www.linkedin.com";
			String domain = ".linkedin.com";
			setACooky(manager, url, domain, "bcookie", "d2375dea-b963-49e4-90f9-71a30eeccd25");
			setACooky(manager, url, domain, "srchId",  "5b40857a-343a-4c49-ac92-e0b5623ce4ae");
			setACooky(manager, url, domain, "leo_auth_token", "LIM:1960788:i:1244505808:f23ae663f37659dd693bc98dfa7e558c11f4532f");
			setACooky(manager, url, domain, "NSC_MC_WT_DTQ_IUUQ", "e2420fbd199f");
			setACooky(manager, url, domain, "NSC_MC_QH_MFP",      "e2420d8429a0");
		}
	}
	private long _minWaitMilli = 1000L;
	private long _lastTime = 0L;
	public enum Force { AUTO, OFF, ON }
	public static Parser setupParser(String target, Force force)
		throws ParserException {
		Parser parser =  null;
		boolean quotaAvailable = WebSiteReader_Quota.takeOneCallIfAllowed(ActionType.SERVLET);
		WebSiteReader_Common wsrc = getInstance();
		
		if (quotaAvailable) {
			if (force == Force.AUTO || force == Force.ON)
				wsrc.setupCookies(force == Force.ON);
			long now = 0;
			long timeLeft = 0;
			while (true) {
				now = Calendar.getInstance().getTimeInMillis();
				timeLeft = wsrc._lastTime + wsrc._minWaitMilli - now;
				if (timeLeft <= 0)
					break;
				try {
					Thread.sleep(timeLeft);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} 
			for (int i = 0; i < 5; i++) {
				try {
					log.log(Level.INFO, "new Parser(" + target + ")");
					parser = new Parser(target);
				}
				catch (RuntimeException e)  {
					e.printStackTrace();
				}
				if (parser != null)
					break;
				if (!WebSiteReader_Quota.isCallAllowed(ActionType.SERVLET))
    				break;
			}
			wsrc._lastTime = Calendar.getInstance().getTimeInMillis();
		}
		return parser;
	}
	public static void startNewHttpRequest() {
		WebSiteReader_Common wsrc = getInstance();
		wsrc.reset();
	}
	
	
		// set up proxying
		/*
		manager.setProxyHost ("proxyhost.mycompany.com");
		manager.setProxyPort (8888);
		manager.setProxyUser ("FredBarnes");
		manager.setProxyPassword ("secret");
		*/
		// set up security to access a password protected URL
		/*
		manager.setUser ("FredB");
		manager.setPassword ("holy$cow");
		*/
		// set up (an inner class) for callbacks
		/*
		ConnectionMonitor monitor = new ConnectionMonitor ()
		    {
		        public void preConnect (HttpURLConnection connection)
		        {
		            System.out.println (HttpHeader.getRequestHeader (connection));
		        }
		        public void postConnect (HttpURLConnection connection)
		        {
		            System.out.println (HttpHeader.getResponseHeader (connection));
		        }
		    };
		manager.setMonitor (monitor);
		*/
		// perform the connection
		//Parser parser = new Parser ("http://frehmeat.net");

	
}
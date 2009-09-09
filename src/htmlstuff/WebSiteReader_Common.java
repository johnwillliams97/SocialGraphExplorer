package htmlstuff;


import java.util.logging.Logger;
import java.util.logging.Level;
import org.htmlparser.Parser;
import org.htmlparser.http.ConnectionManager;
import org.htmlparser.http.Cookie;
import org.htmlparser.util.ParserException;

import people.client.Misc;
import people.client.Statistics;

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
	
	/*
	 * Cookie logic
	 */
	private  boolean _cookiesHaveBeenSet = false;
	private void reset() {
		_cookiesHaveBeenSet = false;
	}
	private static void setACooky(ConnectionManager manager, String url, String domain, 
			String key, String value) {
		Cookie cookie = new Cookie (key, value);
		cookie.setDomain (domain);
		manager.setCookie (cookie, null); // url not needed if domain specified?
	}
	
	/* 
	 * The cookies set by LinkedIn then used to bypass future authentication
	 * You must use your own cookies so that you do not take other people's data
     * The cookies are stored in MagicLICookies.java 
     * You will need to create your own version of MagicLICookies.java to access LinkedIn
 	 * 
	 */
	private void setupCookies(boolean isForced) throws ParserException {
		if (!_cookiesHaveBeenSet || isForced) {
			_cookiesHaveBeenSet = true;
			ConnectionManager manager = Parser.getConnectionManager ();
			log.log(Level.INFO, "setupCookies()");
			// set up cookies
			String url = "www.linkedin.com";
			String domain = ".linkedin.com";
			setACooky(manager, url, domain, "bcookie", MagicLICookies.bcookie);
			setACooky(manager, url, domain, "srchId",  MagicLICookies.srchId);
			setACooky(manager, url, domain, "leo_auth_token",  MagicLICookies.leo_auth_token);
			setACooky(manager, url, domain, "NSC_MC_WT_DTQ_IUUQ",  MagicLICookies.NSC_MC_WT_DTQ_IUUQ);
			setACooky(manager, url, domain, "NSC_MC_QH_MFP",       MagicLICookies.NSC_MC_QH_MFP);
		}
	}
	
	/*
	 * Parsing
	 */
	private double _minWaitSec = 1.0;
	private double _lastTime = 0L;
	public enum Force { AUTO, OFF, ON }
	
	public static Parser setupParser(String target, Force force, double timeBoundSec) 
		throws ParserException {
		Parser parser =  null;
		WebSiteReader_Common wsrc = getInstance();
		
		if (force == Force.AUTO || force == Force.ON)
			wsrc.setupCookies(force == Force.ON);
		double now = 0.0;
		double timeLeft = 0;
		while (true) {
			now = Statistics.getCurrentTime();
			timeLeft = wsrc._lastTime + wsrc._minWaitSec - now;
			if (timeLeft <= 0.0)
				break;
			try {
				Thread.sleep((long)(timeLeft*1000.0));
			} catch (InterruptedException e) {
				Misc.reportException(e);
			}
		} 
		for (int i = 0; i < 5; i++) {
			try {
				log.info("new Parser(" + target + ")");
				parser = new Parser(target);
			}
			catch (RuntimeException e)  {
				Misc.reportException(e);
			}
			if (parser != null)
				break;
			now = Statistics.getCurrentTime();
			if (now >= timeBoundSec)
				break;
		}
		wsrc._lastTime = Statistics.getCurrentTime();
	
		return parser;
	}
	
	public static void startNewHttpRequest() {
		WebSiteReader_Common wsrc = getInstance();
		wsrc.reset();
	}
	
	
}
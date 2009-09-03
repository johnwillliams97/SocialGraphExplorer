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
	 * 
	 * bcookie 			d2375dea-b963-49e4-90f9-71a30eeccd25 www.linkedin.com
	 * srchId  			649ee890-b59e-472c-bb88-86301185d410
	 * leo_auth_token  LIM:1960788:i:1244080991:77a7f084f010a8f668d6f226805e34ef2d6eb60a
	 * NSC_MC_WT_DTQ_IUUQ  e2420fbd199f
	 * NSC_MC_QH_MFP       e2420d8429a0
	 */
	private void setupCookies(boolean isForced) throws ParserException {
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
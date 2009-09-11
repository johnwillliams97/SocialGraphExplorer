
package people.client;


import com.google.gwt.user.client.Window;


/**
 * Odds and ends
 * 
 * http://code.google.com/p/google-web-toolkit-doc-1-5/wiki/FAQ_EnableAssertions
 * "Only use assertions for debugging purposes, not production logic because assertions 
 * will only work under GWT's hosted mode. They are compiled away by the GWT compiler 
 * so do not have any effect in web mode." NOT HELPFUL!
 */
public class Misc {
	
	public static void myAssert(boolean condition, String msg) {
		if (!condition) {
			assert(condition);  // Add breakpoint here
			Window.alert("assertion failed!" + (msg != null ? (": " + msg) : ""));
		}
	}
	
	public static void reportException (Exception e) {
		if (!OurConfiguration.SUPRESS_ERRORS) {
			e.printStackTrace();
		}
	}
	
	private static final int KBYTE = 1024;
	private static final int MBYTE = KBYTE * KBYTE;
	
	public static String showBytes(int bytes) {
		String s = null;
		if (bytes < 2 * KBYTE)
			s = "" + bytes + "bytes";
		else if (bytes < 2 * MBYTE)
			s = "" + bytes/KBYTE + "kb";
		else
			s = "" + bytes/MBYTE + "mb";
		return s;
	}
	/*
	static public String decodeUrl(String description) {
  		String descClean = null;
  		if (description != null) {
			try {
				descClean = URLDecoder.decode(description, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				Misc.reportException(e);
			}
			while (descClean.contains("&amp;")) {
				descClean = descClean.replaceAll("&amp;", "&");
			}
			while (descClean.contains("&quot;")) {
				descClean = descClean.replaceAll("&quot;", "'");
			}
  		}
  		return descClean;
  	}
  	*/
}

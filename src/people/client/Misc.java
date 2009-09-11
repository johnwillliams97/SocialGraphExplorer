
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
		if (bytes < KBYTE)
			s = "" + bytes + " bytes";
		else if (bytes < MBYTE)
			s = "" + round1((double)bytes/(double)KBYTE) + " kb";
		else
			s = "" + round1((double)bytes/(double)MBYTE) + " mb";
		return s;
	}
		
	public static double round1(double x) {
		return roundBy(x, 10.0);
	}
	public static double round3(double x) {
		return roundBy(x, 1000.0);
	}
	private static double roundBy(double x, double multiplier) {
		return ((double)Math.round(x*multiplier))/multiplier;
	}
}


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
	
	public static void myAssert(boolean condition) {
		myAssert(condition, null);
	}
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
	
}

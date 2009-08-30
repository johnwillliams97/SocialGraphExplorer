package htmlstuff;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.Logger;
import org.htmlparser.*;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import htmlstuff.WebSiteReader_Common.Force;
import datatypes.PersonLI;

/*
 * LinkedIn key  
 * 		<meta name="UniqueID" content="258598">
 * Profile
 * 		http://www.linkedin.com/profile?viewProfile=&key=258598
 *  		<meta name="LinkedInBookmarkType" content="profile">
 *  		<meta name="ShortTitle" content="Will Cave-Bigley">
 *  		<meta name="Description" content="Will Cave-Bigley: Manager- Ecareer in the Staffing and Recruiting industry (Melbourne Area, Australia)">
 *  		<meta name="UniqueID" content="258598">
 *  		<meta name="SaveURL" content="http://www.linkedin.com/profile?viewProfile=&amp;key=258598&amp;authToken=BMgJ&amp;authType=name"> 

	"See all Connections"
		<div class="module connections"> 
    	<div class="header"> 
		<p class="more">
			<a href="/profile?viewConns=&key=258598&goback=%2Evpf_258598_1_BMgJ_name_*1_Will_Cave*5Bigley" 
				name="seeMoreConnections">See all <strong>Connections</strong> &raquo;</a></p> 

 * List of connections
 *		http://www.linkedin.com/profile?viewConns=&key=258598
 *          <div class="cnxset"> 
          	<div class="cnxset-in"> 
            <div class="listbox" id="other"><div class="listbox-in"> 
            <ul name="browseConnectionsGrid"> 
            	<li class="row-start connection">
            		<a href="/profile?viewProfile=&key=2532782&goback=%2Ebcc_258598_1" title="View Alex's profile" rel="contact">Alex Adams</a> 
                    <br><span name="headline" class="headline">IT Executive</span></li> 
               	<li class=" connection">
               		<a href="/profile?viewProfile=&key=5862348&goback=%2Ebcc_258598_1" title="View Allan's profile" rel="contact">Allan Davies</a> 
                    <br><span name="headline" class="headline">Delivery Assurance Consultant at Transurban</span></li
 *
 * 
 */
public class WebSiteReader_UserProfile {
	private static final Logger logger = Logger.getLogger(WebSiteReader_UserProfile.class.getName());
	
	private long _timeBoundMillis = 0L;  // For time-bound best-effort operation
	
	
	public WebSiteReader_UserProfile(long timeBoundMillis) {
		_timeBoundMillis = timeBoundMillis;
	}
	
	
	@SuppressWarnings("unused")
	private String _target = null; // for debugging
	
	static private String _linkedInBase = "http://www.linkedin.com/";
	static String makeLiProfileName(long key) {
		String name = _linkedInBase + "profile?viewProfile=&key=" + key;
		return name;		
	}
	static String makeLiConnectionsName(long key) {
		String name = _linkedInBase + "profile?viewConns=&key=" + key;
		return name;		
	}
	/*
	 * http://htmlparser.sourceforge.net/samples.html
	 * http://htmlparser.sourceforge.net/javadoc/org/htmlparser/Parser.html#main(java.lang.String[])
	 */
	
	/* Profile
	 * 		http://www.linkedin.com/profile?viewProfile=&key=258598
	 *  		<meta name="LinkedInBookmarkType" content="profile">
	 *  		<meta name="ShortTitle" content="Will Cave-Bigley">
	 *  		<meta name="Description" content="Will Cave-Bigley: Manager- Ecareer in the Staffing and Recruiting industry (Melbourne Area, Australia)">
	 *  		<meta name="UniqueID" content="258598">
	 */ 
		static private void setPersonFromLiTitle(PersonLI person, String shortTitle) {
			person.setNameFull(shortTitle);
			String[] subStrings = shortTitle.split("\\s");
			int  numSubStrings = 0;
			if (subStrings != null) {
				numSubStrings = subStrings.length;
				if (numSubStrings > 0)
					person.setNameFirst(subStrings[0]);
				if (numSubStrings > 1)
					person.setNameLast(subStrings[numSubStrings-1]);
			}
		}
		
		static private void setPersonFromLiDescription(PersonLI person, String description) {
			String descMeat = description;
			String descMeatClean = null;
			if (description != null) {
				if (description.length() > 400)
					  logger.info("description (b)[" + description.length()+ "] " + description);
			
				int colonIndex = description.indexOf(':') ; 
				if (colonIndex >= 0)
					descMeat = description.substring(colonIndex+1);
				descMeat.trim();
				try {
					descMeatClean = URLDecoder.decode(descMeat, "UTF-8"); // unescape the description
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} 
				String[] subStrings = descMeatClean.split("[()]"); // ("\\x28\\x29"); // ("\\(\\)");
				int  numSubStrings = 0;
				if (subStrings != null) {
					numSubStrings = subStrings.length;
					if (numSubStrings > 0) {
						person.setDescription(subStrings[0]);
						person.setLocation(subStrings[1]);
					}
				}
				else {
					person.setDescription(descMeatClean);
				}
			}
		}
		
		static private void setHasMoreConnections(PersonLI person, boolean hasMoreConnections) {
			person.setChildConnectionProgress(PersonLI.CHILDREN_CONNECTIONS, 
					hasMoreConnections ? PersonLI.PROGRESS_NOT_STARTED : PersonLI.PROGRESS_COMPLETED );
		}
	/*
	 * Search state for one profile html page
	 */
		private PersonLI  _person = new PersonLI(); 	// Built up internally
		private PersonLI  _outPerson =  null;		// The PersonLI returned. == _person after a successful read
		private boolean   _foundTitle = false;
		private boolean   _foundDescription = false;
		private boolean   _foundUniqueID = false;
		private boolean isDoneSearchingProfile() {
			return _foundTitle && _foundDescription && _foundUniqueID;
		}
		
	void processNodesLiProfile (Node node) 
				throws ParserException, IOException	 {
			
			if (isDoneSearchingProfile())
				return;
			
			TagNode tag = null;
			String tagName = null;
			String tagNameAttr = null;
			String shortTitle = null;
			String description = null;
			String uniqueID  =  null;
	    	if (node instanceof TagNode)    {
	    		tag = (TagNode)node;
	            tagName = tag.getTagName();
	            if (tagName != null && tagName.equalsIgnoreCase("meta")){
		            tagNameAttr = tag.getAttribute("name");
		    	}
	    	}
	    	
	    	if (tagNameAttr != null ) {
	            if (tagNameAttr.equalsIgnoreCase("ShortTitle")) {
	            	shortTitle = tag.getAttribute("content");
	            	if (shortTitle != null) {
	            		setPersonFromLiTitle(_person, shortTitle);
	            		_foundTitle = true;
	            	}
	            }
	            if (tagNameAttr.equalsIgnoreCase("Description")) {
	            	description = tag.getAttribute("content");
	           // 	Attribute a = tag.getAttributeEx("content");
	            	if (description != null) {
	            		setPersonFromLiDescription(_person, description);
	            		_foundDescription = true;
	            	}
	            }
	            if (tagNameAttr.equalsIgnoreCase("UniqueID")) {
	            	uniqueID = tag.getAttribute("content");
	            	if (uniqueID != null) {
	            		assert(_person.getLiUniqueID() == Long.getLong(uniqueID));
	            		_foundUniqueID = true;
	            	}
	            }
	    	}
		}    
	
	/*
	 * "See all Connections"
			<div class="module connections"> 
	    	<div class="header"> 
			<p class="more">
				<a href="/profile?viewConns=&key=258598&goback=%2Evpf_258598_1_BMgJ_name_*1_Will_Cave*5Bigley" 
					name="seeMoreConnections">See all <strong>Connections</strong> &raquo;</a></p> 	
	 */
	private boolean _hasMoreConnections = false;	
	
	void processNodesLiHasConnections (Node node) 
		throws ParserException, IOException	 {
	
		if (_hasMoreConnections)
			return;
	
		TagNode tag = null;
		String tagName = null;
		String tagNameAttr = null;
		if (node instanceof TagNode)    {
			tag = (TagNode)node;
	        tagName = tag.getTagName();
	        if (tagName != null && tagName.equalsIgnoreCase("A")){
	            tagNameAttr = tag.getAttribute("name");
	            if (tagNameAttr != null ){
	    	        if (tagNameAttr.equalsIgnoreCase("seeMoreConnections")) {
	    	        	setHasMoreConnections(_person, true);	// Mark connection as incomplete
	    	        	_hasMoreConnections = true;
	    	        }
	    		}
	    	}
		}
  	} 
		
		/*
		 * Overall tag processing
		 */
	private boolean isDoneSearching() {
		return isDoneSearchingProfile() && _hasMoreConnections;
	}
	void processNodesLi(Node node, int depth) 
		throws ParserException, IOException	 {
		
		processNodesLiProfile(node);
		processNodesLiHasConnections(node);
		
		TagNode tag = null;
    	if (node instanceof TagNode)    {  
    		tag = (TagNode)node;
         // process recursively (nodes within nodes) via getChildren()
    		NodeList nl = tag.getChildren ();
	         if (nl != null) {
	             for (NodeIterator itr = nl.elements ();  itr.hasMoreNodes(); ) {
	            	 processNodesLi (itr.nextNode(), depth+1);
	             }
	         }
    	}
	}
	
	/* 	Program flow
	 * 	FindLinks(<liUniqueID, depth, maxDepth>) {
	 * 		Get http://www.linkedin.com/profile?viewProfile=&key=<liUniqueID>
	 * 		Save details to database
	 * 		if ("See all Connections »" enabled) && (depth < maxDepth)
	 *  		for all links <L> in http://www.linkedin.com/profile?viewConns=&key=<liUniqueID>
	 *  			FindLinks(<L, depth+1, maxDepth>)
	 *  }
	 *  
	 *  http://www.linkedin.com/profile?viewProfile=&key=2532782
	 * 	http://www.linkedin.com/profile?viewConns=&key=2532782
	 */
	public static String makeLiProfileUrl(long liUniqueID) {
		String url = "http://www.linkedin.com/profile?viewProfile=&key=" + liUniqueID;
		return url;
	}
	public static  String makeLiConnectionsUrl(long liUniqueID) {
		String url = "http://www.linkedin.com/profile?viewConns=&key=" + liUniqueID;
		return url;
	}
	/*
	 * Main function. Gets LinkedIn user profile info
	 */
	String  _wholePageBuffer = null;
	
	private void doMakeLiUserProfile(long liUniqueID, 
									 boolean parsePage,
									 boolean fetchWholePage)  
		throws NullPointerException {
		String target = makeLiProfileUrl(liUniqueID);
		_target = target; // for debugging
	
		_person.setLiUniqueID(liUniqueID);
	
		setHasMoreConnections(_person, false);  // Until proven otherwise
	
		try { 
			Parser parser = WebSiteReader_Common.setupParser(target, Force.ON, _timeBoundMillis );
			if (parser != null) {
				if (fetchWholePage) {
					// Save whole page of HTML
					 NodeList nl = parser.parse(null);
					 _wholePageBuffer = nl.toHtml();		// Success !
				}
				if (parsePage) {
					// If page has already been read from web then re-use it
					if (_wholePageBuffer != null) {
						parser.setInputHTML(_wholePageBuffer);
					}
					// Parse the page
					for (NodeIterator i = parser.elements(); i.hasMoreNodes() && !isDoneSearching(); ) {
						processNodesLi(i.nextNode(), 0); 
					} 
					_outPerson = _person; 					// Success !
				}
			}
		} 
		catch (ParserException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}  
	}  
	/*
	 * 2 public methods $%^&
	 * 	doGetLiUserProfile() gets a summary of the person
	 * 	doGetLiUserProfilePage() fetches the whole page
	 */
	public static PersonLI doGetLiUserProfile(long liUniqueID, long timeBoundMillis)  {
		PersonLI person = null;
		boolean fetchWholePage = false;
		try {
			WebSiteReader_UserProfile wsr = new WebSiteReader_UserProfile(timeBoundMillis);
			wsr.doMakeLiUserProfile(liUniqueID,true, fetchWholePage) ;
			person = wsr._outPerson;
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return person;
	}
	public static String doGetLiUserProfilePage(long liUniqueID, long timeBoundMillis)  {
		WebSiteReader_UserProfile wsr = new WebSiteReader_UserProfile(timeBoundMillis);
		wsr.doMakeLiUserProfile(liUniqueID, false, true) ;
		return wsr._wholePageBuffer;
	}
	
}
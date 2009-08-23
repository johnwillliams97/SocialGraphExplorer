package htmlstuff;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.htmlparser.*;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import datatypes.PersonLI;
import htmlstuff.WebSiteReader_Common.Force;

public class WebSiteReader_Connections {

	@SuppressWarnings("unused")
	private String _target = null; // for debugging
	
	static private String _linkedInBase = "http://www.linkedin.com";
	public static String makeLiConnectionsName(long key) {
		String name = _linkedInBase + "/profile?viewConns=&key=" + key;
		return name;		
	}
	
	
	
	/*
	 * Search state for one profile html page
		 */
	private List<Long> _connectionKeys = new ArrayList<Long>();
	
	/*
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
		 */
	private boolean isDoneSearching() {
		return false;
	}
	private final static String _profile_prefix = "/profile?viewProfile=&key=";
	private final static int    _profile_prefix_len =  _profile_prefix.length();
	private void processNodesLiConnections(Node node) 
			throws ParserException, IOException	 {
			
		if (isDoneSearching())
			return;
			
		TagNode tag = null;
		String  tagName = null;
		String  tagNameAttr = null;
		String  suffix = null;
		String[] keyStrings = null;
		long    key = -1;
		if (node instanceof TagNode)    {
    		tag = (TagNode)node;
            tagName = tag.getTagName();
            if (tagName != null && tagName.equalsIgnoreCase("A")){
	            tagNameAttr = tag.getAttribute("href");
	            if (tagNameAttr != null ){
		            if (tagNameAttr.startsWith(_profile_prefix)) {
		            	suffix = tagNameAttr.substring(_profile_prefix_len);
		            	keyStrings = suffix.split("&");
		            	if (keyStrings != null && keyStrings.length >= 1) {
		            		key = Long.parseLong(keyStrings[0]);
		            		if (key > 0L) {
		            			_connectionKeys.add(key);
		            		}
		            	}
		            }
	            }
	    	}
    	}
		
  	}    
	
	/*
	 *  <p class="page">Page: 
	 *      <a href="/profile?viewConns=&key=134794&split_page=3" name="_previous"><strong>&#171;&nbsp;previous</a></strong> 
	 *  	<a href="/profile?viewConns=&key=134794&split_page=1" >1</a> 
	 *  	<a href="/profile?viewConns=&key=134794&split_page=2" >2</a> 
	 *  	<a href="/profile?viewConns=&key=134794&split_page=3" >3</a>
	 *   	<strong>4</strong> <a href="/profile?viewConns=&key=134794&split_page=5" >5</a> 
	 *   		<a href="/profile?viewConns=&key=134794&split_page=5" name="_next"><strong>next&nbsp;&#187;	
	 *   	</strong></a>&nbsp;&nbsp;
	 *   </p> 
	 */
	private final static String _split_prefix = "/profile?viewConns=&key=";
	private String _split_nextPage = null;
	private boolean _inPageClass = false;
	private void processNodesLiConnectionsSplit(Node node) 
		throws ParserException, IOException	 {
		if (isDoneSearching())
			return;
		TagNode tag = null;
		String  tagName = null;
		String  tagNameAttrClass = null;
		String  tagNameAttrHref = null;
		String  tagNameAttrName = null;
		if (node instanceof TagNode)    {
			tag = (TagNode)node;
		    tagName = tag.getTagName();
		    if (tagName != null && tagName.equalsIgnoreCase("P")){
		    	tagNameAttrClass = tag.getAttribute("class");
		        if (tagNameAttrClass != null ){
		            if (tagNameAttrClass.equalsIgnoreCase("page")) {
		            	_inPageClass = !tag.isEndTag();
		            }
		        }
			}
		    else if (tagName != null && _inPageClass && tagName.equalsIgnoreCase("A") ){
		    	tagNameAttrName = tag.getAttribute("name");
		        if (tagNameAttrName != null ){
		      		if (tagNameAttrName.equalsIgnoreCase("_next")) {
		      			tagNameAttrHref = tag.getAttribute("href");
				        if (tagNameAttrHref != null ){
				        	if (tagNameAttrHref.startsWith(_split_prefix)) {
				        		_split_nextPage = _linkedInBase + tagNameAttrHref;
					       	}
				        }
			        }
		        }
			}
		}
	}  
		
	/*
	 * Overall tag processing
	 */
	
	void processNodes(Node node, int depth) 
		throws ParserException, IOException	 {
		
		if (isDoneSearching())
			return;
		
		processNodesLiConnections(node);
		processNodesLiConnectionsSplit(node) ;
		
		TagNode tag = null;
    	if (node instanceof TagNode)    {  
    		tag = (TagNode)node;
         // process recursively (nodes within nodes) via getChildren()
    		NodeList nl = tag.getChildren ();
	         if (nl != null) {
	             for (NodeIterator itr = nl.elements ();  itr.hasMoreNodes(); ) {
	            	 processNodes (itr.nextNode(), depth+1);
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
	// http://www.linkedin.com/profile?viewConns=&key=2532782&split_page=2
	public static String makeLiConnectionsUrl(long liUniqueID, long partPageNum) {
		String url = null;
		if (partPageNum < 0)  {  // No part page
			url = "http://www.linkedin.com/profile?viewConns=&key=" + liUniqueID;
		}
		else {				  // Part page
			url = "http://www.linkedin.com/profile?viewConns=&key=" + liUniqueID
			    + "&split_page=" + partPageNum;
		}
		return url;
	}
	/*
	 * Main function. Gets LinkedIn user profile info
	 * 
	 */
	boolean _fetchWholePage = false;
	String  _wholePageBuffer = null;
	boolean _connected      = false;
	
	private String doMakeLiConnectionsOneSplit(String target) 
	     throws NullPointerException {
		_target = target; // for debugging
	
		try { 
			Parser parser = WebSiteReader_Common.setupParser(target, Force.ON);
			if (parser != null) {
				_connected = true;
				if (_fetchWholePage) {
					 NodeList nl = parser.parse(null);
					 _wholePageBuffer = nl.toHtml();
				}
				else {
					for (NodeIterator i = parser.elements(); i.hasMoreNodes() && !isDoneSearching(); ) {
						processNodes(i.nextNode(), 0); 
					}   
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
			assert(false);
		}
		 
		return _split_nextPage;
	}
	
		
	
	/*	Read all connection subpages, possibly starting part way
	 * 		lastPageNumRead = last page number read in a previous read, 
	 * 		or PersonLI.PROGRESS_NOT_STARTED if this is the first read
	 * 
	 * 	Need to handle partial lists
			-1 = no pages read
			 0 = PersonLI.PROGRESS_COMPLETED
			>0 = number of pages read
			need to distinguish between no attempt to read and failure
	 */
	public static ConnectionsReaderState  doGetLiConnections(long liUniqueID, long lastPageNumRead)  {
		assert(lastPageNumRead != PersonLI.PROGRESS_COMPLETED);  // This function should not be called if the person's connection list is complete
		assert(lastPageNumRead == PersonLI.PROGRESS_NOT_STARTED || lastPageNumRead > 0);

		ConnectionsReaderState readerState = new ConnectionsReaderState(); 
		readerState.firstPageNumRead = PersonLI.PROGRESS_NOT_STARTED; // Nothing read so far
		readerState.lastPageNumRead = PersonLI.PROGRESS_NOT_STARTED; // Nothing read so far
		
		long firstPageNumRead = (lastPageNumRead == PersonLI.PROGRESS_NOT_STARTED) ? 1 : lastPageNumRead + 1 ;
		long currPageNumRead = firstPageNumRead;
		String nextPage = makeLiConnectionsUrl(liUniqueID, currPageNumRead);
		int numPasses = 0;
		
		do {
			WebSiteReader_Connections wsr = new WebSiteReader_Connections();
			wsr.doMakeLiConnectionsOneSplit(nextPage);
			if (!wsr._connected)
				break;
			readerState.connected = true;
			readerState.firstPageNumRead = firstPageNumRead; // First read has succeeded
			readerState.lastPageNumRead = currPageNumRead;
			
			nextPage = wsr._split_nextPage;
			if (nextPage == null) {
				readerState.lastPageNumRead = PersonLI.PROGRESS_COMPLETED;
				readerState.hasReadAllPages = true;
			}
			else {
				++currPageNumRead;
			}
			if (wsr._connectionKeys != null) {
				if (readerState.connectionKeys == null)
					readerState.connectionKeys = wsr._connectionKeys;
				else
					readerState.connectionKeys.addAll(wsr._connectionKeys);
			}
			++numPasses;
		} while (nextPage != null && numPasses < 1);  		// limits this to 1 pass
		return readerState.connected ? readerState : null;
	}
	public static String doGetLiConnectionsPage(long liUniqueID, long lastPageNumRead)  {
		WebSiteReader_Connections wsr = new WebSiteReader_Connections();
		wsr._fetchWholePage = true;
		String nextPage = makeLiConnectionsUrl(liUniqueID,  lastPageNumRead);
		wsr.doMakeLiConnectionsOneSplit(nextPage) ;
		return wsr._wholePageBuffer;
	}
	
}
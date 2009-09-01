package people.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;

import people.client.PersonClientCache.GetPersonsFromCacheCallback;
import people.client.Interval;


/**
 * 
 * The main UI for SocialGraphExplorer
 * 	A composite that displays a list of persons that can be selected.
 * 
 * Behaviour
 *  Each person has a list of connections.
 * 	First person in list is anchor
 *  Other people are anchor's connections (in anchor list order)
 *  Clicking on a person highlights them and brings their details into the detail pain
 *  Clicking on a highlighted person makes them the anchor
 *  
 *  Widgets
 *  	Anchor history list
 *  	First, last, next, previous
 *  
 *  Caching
 *  	All data is fetched from a client cache.
 *  	There are two ways of interacting with the cache
 *  	1) Update and read - updateCacheAndGetVisible(long[][] uniqueIDsList)  
 *  	2) Read only
 *  
 *  Bootstrapping
 *  	Loading anchor connection list requires one server call, so at startup
 *  	the client cache must be read twice: (2 client server round trips)
 *  		get anchor 
 *  		get anchor's connections	
 *  
 *  References
 * 		http://www.zackgrossbart.com/hackito/antiptrn-gwt/ some key design principles
 */
public class PersonList extends Composite implements ClickHandler {
	
	// The person cache
	private final PersonClientCache personClientCache;
	
	// UI behaviour
	static final int VISIBLE_PERSONS_COUNT = OurConfiguration.VISIBLE_PERSONS_COUNT;  // %^&* 10
	
	// When a person is first fetched from the cache, a 2nd call will be required to 
	// fetch all that persons connections.
	private boolean _needs2ndCacheCall = true; // !@#$ Move to CanonicalState
	/*
	 * This UI is a list. First item in the list is the 'anchor'.
	 * The following items are indexes in anchor's connections list
	 * anchorUniqueID, startIndex and selectedRow completely describe the state of this clss; 
	 */
	
	private CanonicalState state = new CanonicalState();
	private CanonicalState oldState = new CanonicalState();;  // For tracking changes !@#$ is this needed?
	
	private int	selectedRow = -1; 
	
	// History list for anchor !@#$ Create or find a history list class. 
	static final int ANCHOR_HISTORY_COUNT = OurConfiguration.ANCHOR_HISTORY_COUNT; // 2;
	private StackLikeContainer<PersonClient> oldAnchors = new StackLikeContainer<PersonClient>(ANCHOR_HISTORY_COUNT);
	private StackLikeContainer<PersonClient> newAnchors = new StackLikeContainer<PersonClient>(ANCHOR_HISTORY_COUNT);
	
	// Unique IDs of all persons tracked in this class
	private long[][] uniqueIDsList = null;
	
	// Visible persons data. This gets copied from the client cache.
	private PersonClient theAnchor = null;
	private List<PersonClient> visiblePersons = new ArrayList<PersonClient>();;
	
	
	// Widgets
	private HTML countLabel  = new HTML();
	private HTML newerButton = new HTML("<a href='javascript:;'>&lt;</a>", true);
	private HTML olderButton = new HTML("<a href='javascript:;'>&gt;</a>", true);
	private HTML newestButton = new HTML("<a href='javascript:;'>&lt;&lt;</a>", true);
	private HTML oldestButton = new HTML("<a href='javascript:;'>&gt;&gt;</a>", true);
	private HTML backButton   = new HTML("<a href='javascript:;'>*</a>", true);
	
	private FlexTable table = new FlexTable();
	private HorizontalPanel navBar = new HorizontalPanel();

	private boolean isNavigationDisabled = false;

	static int debug_num_calls = 0; // !@#$ Why is PersonList() getting called multiple times???
  	/*
  	 * Set up the list UI
  	 */
	public PersonList() {
		// GWT sometimes calls this twice in hosted mode.
		++debug_num_calls;
		if (debug_num_calls > 1) {
			assert(debug_num_calls <= 1);
		}
		
		// Setup the cache
		int[] cacheLevelSize = new int[PersonClientCache.CACHE_LEVEL_NUMBER_LEVELS];
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_ANCHOR] = 1 /*+ 2*ANCHOR_HISTORY_COUNT*/;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_VISIBLE] = VISIBLE_PERSONS_COUNT - 1;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK1] = (4 + VISIBLE_PERSONS_COUNT - 1) * VISIBLE_PERSONS_COUNT;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK2] = (2) * VISIBLE_PERSONS_COUNT;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_RECENT] = OurConfiguration.CACHE_SIZE_LRU;
		personClientCache = new PersonClientCache(cacheLevelSize);
	
		// Setup the table UI.
		table.setCellSpacing(0);
		table.setCellPadding(0);
		table.setWidth("100%");

		// Hook up events.
		table.addClickHandler(this);
		backButton.addClickHandler(this);
		newerButton.addClickHandler(this);
		olderButton.addClickHandler(this);
		newestButton.addClickHandler(this);
		oldestButton.addClickHandler(this);

		// Create the 'navigation' bar at the upper-right.
		HorizontalPanel innerNavBar = new HorizontalPanel();
		navBar.setStyleName("mail-ListNavBar");
		innerNavBar.add(backButton);
		innerNavBar.add(newestButton);
		innerNavBar.add(newerButton);
		innerNavBar.add(olderButton);
		innerNavBar.add(oldestButton);
		innerNavBar.add(countLabel);

		navBar.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
		navBar.add(innerNavBar);
		navBar.setWidth("100%");

		initWidget(table);
		setStyleName("mail-List");

    	initTable();
    
    	/*
    	 *  Initialise data. !@#$ Need to call twice. See above.
    	 *  	First call fetches a server person for the anchor
    	 *  	Second call fetches anchor's connections
    	 * 	this.state.anchorFetched is used to do this, after the cache returns from updatePersonList() in
    	 * 	cacheCallbackUpdateList.handleReturn()
    	 * 
    	 */
    	
    	//updatePersonList("Constructor");
    	updatePersonListExtern(this.state.getAsString(), false);  //<= Instead !@#$
	}

	@Override
	protected void onLoad() { // !@#$
	//	dynaTable.refresh(true);
	}
  /**
   * Initialise the table so that it contains enough rows for a full page of
   * persons. 
   */
	private void initTable() {
       table.setWidget(0, 3, navBar);
       table.getRowFormatter().setStyleName(0, "mail-ListHeader");

       // Initialise the rest of the rows.
       for (int i = 0; i < VISIBLE_PERSONS_COUNT; ++i) {
	      table.setText(i + 1, 0, "");
	      table.setText(i + 1, 1, "");
	      table.setText(i + 1, 2, "");
	      table.getCellFormatter().setWordWrap(i + 1, 0, false);
	      table.getCellFormatter().setWordWrap(i + 1, 1, false);
	      table.getCellFormatter().setWordWrap(i + 1, 2, false);
	      table.getFlexCellFormatter().setColSpan(i + 1, 2, 2);
       }
	}

	/*
	 * (non-Javadoc)
	 * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
	 * Respond to UI events
	 * 
	 * The state of the UI is completely specified by this.state.anchorUniqueID and this.startIndex (and possibly 
	 * incomplete fetches)
	 */
	@Override
	public void onClick(ClickEvent event) {
		//SocialGraphExplorer.get().showInstantStatus("onClick()");
	    Object sender = event.getSource();
	    if (sender == olderButton) {
	    //	SocialGraphExplorer.get().showInstantStatus("olderButton");
	      // Move forward a page.
	      this.state.startIndex += VISIBLE_PERSONS_COUNT;
	      if (this.state.startIndex >= getPersonCount()) {
	        this.state.startIndex -= VISIBLE_PERSONS_COUNT;
	      } else {
	        styleRow(selectedRow, false);
	        selectedRow = -1;
	        updatePersonList("olderButton");
	      }
	    } 
	    else if (sender == newerButton) {
	    //	SocialGraphExplorer.get().showInstantStatus("newerButton");
	      // Move back a page.
	      this.state.startIndex -= VISIBLE_PERSONS_COUNT;
	      if (this.state.startIndex < 0) {
	        this.state.startIndex = 0;
	      } else {
	        styleRow(selectedRow, false);
	        selectedRow = -1;
	        updatePersonList("newerButton");
	      }
	    } 
	    else if (sender == oldestButton) {
	    	//SocialGraphExplorer.get().showInstantStatus("oldestButton");
		    // Move to end.
		    this.state.startIndex = getPersonCount() -1 -VISIBLE_PERSONS_COUNT;
		    if (this.state.startIndex < 0) 
		        this.state.startIndex = 0;
		    styleRow(selectedRow, false);
		    selectedRow = -1;
		    updatePersonList("oldestButton");
		}
		else if (sender == newestButton) {
			//SocialGraphExplorer.get().showInstantStatus("newestButton");
		    // Move to start.
		    this.state.startIndex = 0;
		    styleRow(selectedRow, false);
		    selectedRow = -1;
		    updatePersonList("newestButton");
		} 
	     else if (sender == backButton) {
	    	 SocialGraphExplorer.get().showInstantStatus("backButton");
		    // Move to start.
		    styleRow(selectedRow, false);
		    rewindAnchor();
		    selectedRow = -1;
		    updatePersonList("backButton");
		}
	    else if (sender == table) {
	    	//  SocialGraphExplorer.get().showInstantStatus("table ");
	      // Select the row that was clicked (-1 to account for header row).
	      Cell cell = table.getCellForEvent(event);
	      if (cell != null && !this.isNavigationDisabled ) {
	    	int row = cell.getRowIndex() - 1;
	        if (row >= 0 && getPersonForRow(row) != null) {
	        	SocialGraphExplorer.get().showInstantStatus("table row " + row);
	        	if (selectedRow != row) {
	        		selectRow(row);		// Change selected row
	        	}
	        	else  {
	        		styleRow(selectedRow, false);
	        		// Set anchor state to desired state. Will be invalid until this.state.anchorFetched is set to true.
	        		// This is a signal that cache state is invalid.
	        	//	long oldID = this.state.anchorUniqueID;
	        		/*
	        		this.state.startIndex = 0;
	        		updateAnchor(getPersonForRow(row), true); // Make this person the anchor
	             	For consistency, use one method to update person
	             	*/
	        		selectedRow = -1;
	        		CanonicalState newState = new CanonicalState(getPersonForRow(row).getLiUniqueID(), 0);
	             	updatePersonListExtern(newState.getAsString(), false);
	        		//updatePersonList("new anchor = " + oldID + " => " + newState.getAsString());
	        	}
	        }
	      }
	    }
	}
	
  	/*
  	 * Get number of people in list. 
  	 * Currently this is the anchor person's number of connection
  	 */
  	private int getPersonCount() {
  		int personCount = 0;
  		PersonClient person = getAnchor();
  		if (person != null && person != PersonClient.MAGIC_PERSON_CLIENT_1) {
  			List<Long> connectionIDs = person.getConnectionIDs();
  			personCount = ((connectionIDs != null) ? connectionIDs.size() : 0) + 1;
  		}
  		// Still in bootstrap?
  		else if (person == PersonClient.MAGIC_PERSON_CLIENT_1) {
  			personCount = 1; 
  		}
  		else {
  			personCount = -1; // For debugging obviously
  		}
  		return personCount;
  	}

  	/*
  	 * Get the current anchor person
  	 */
  	private PersonClient getAnchor() {
  		return this.theAnchor;
  	}
  	/*
     * Update anchor person to the new one fetched from the cache
    */
  	private void updateAnchor(PersonClient newAnchor, boolean saveOldAnchor) {
  		if (newAnchor != null && !newAnchor.isMagicPerson()) { 	// Update to a real person?
		//	if (this.state.anchorUniqueID != newAnchor.getLiUniqueID()) {	// Anything changed? !@#$ Use !this.state.anchorFetched for conditional
			if (!this.state.anchorFetched)	{
				SocialGraphExplorer.get().showInstantStatus("updateAnchor(" + this.oldState.anchorUniqueID + " => " 
						+ newAnchor.getLiUniqueID() + "," + saveOldAnchor + ")");
				if (this.theAnchor != null && !this.theAnchor.isMagicPerson() && saveOldAnchor)
					oldAnchors.push(this.theAnchor);
				SocialGraphExplorer.get().log("oldAnchors", oldAnchors.getState());
				this.theAnchor = newAnchor;
				this.state.anchorUniqueID = newAnchor.getLiUniqueID();
				this.state.anchorFetched = true;
				this.state.visibleFetched = false;   // Visible list is invalid because anchor has changed
				oldState.anchorUniqueID = state.anchorUniqueID;  // !@#$ could do better!
			}
		}
  		
 	}
  	
    	
  	/*
  	 * Backup to previous anchor
  	 */
  	private void rewindAnchor() {
  		// If there are old anchors
  		PersonClient oldAnchor = this.getAnchor();
  		PersonClient newAnchor = this.getAnchor();
  		if (oldAnchors.size() > 0) {
  			// Save current anchorUniqueID in newAnchorUniqueIDs[]
  			newAnchors.push(oldAnchor);
  			// Fetch previous anchorUniqueID
  			newAnchor = oldAnchors.pop();
  			updateAnchor(newAnchor, false);
  		}
  		SocialGraphExplorer.get().showInstantStatus("rewindAnchor(" + oldAnchor.getLiUniqueID() + " => " + this.state.anchorUniqueID +")");
  	}
  	private boolean isOldAnchors() {
  		return (oldAnchors.size() > 0);
  	}
  	
	private void updateVisiblePersonsFromCache(PersonClientCacheEntry[] entries)  {
  		if (entries != null && entries.length > 0) {
	  		long[] ids = this.uniqueIDsList[PersonClientCache.CACHE_LEVEL_VISIBLE];
	  		List<PersonClient> newVisiblePersons = new ArrayList<PersonClient>();
	  		if (ids != null) {
		  		for (int i = 0; i < ids.length; ++i) {
		  			if (ids[i] != PersonClient.UNIQUE_ID_NOT_FOUND) {
		   				for (int j = 0; j < entries.length; ++j) {
		   					if (ids[i] == entries[j].getUniqueID()) {
		   	  					PersonClient p = entries[j].getPerson();
		   	  					newVisiblePersons.add(p);
		   	  					break;
		   					}
		  				}
		  			}
		  		}
	  		}
	  		this.visiblePersons = newVisiblePersons;
  		}
 	}
  	
  	  	
  /**
   * Selects the given row (relative to the current page).
   * @param row the row to be selected
   */
  private void selectRow(int row) {
	// When a row (other than the first one, which is used as a header) is
	// selected, display its associated PersonItem.
	PersonClient person = getPersonForRow(row);
	
	String dbgNameFull = "?!@";
	long   dbgUniqueID = PersonClient.UNIQUE_ID_NOT_FOUND;
	if (person != null) {
		dbgNameFull = person.getNameFull() ;
		dbgUniqueID = person.getLiUniqueID();
	}
	String cons = "[";
	if (person != null) {
		List<Long> conIDs = person.getConnectionIDs();
		if (conIDs != null) {
			int numConnections = Math.min(conIDs.size(), VISIBLE_PERSONS_COUNT);
			for (int i = 0; i < numConnections; ++i)
				cons += "" + conIDs.get(i) + ",";
		}
	}
	cons += "]";
	System.err.println("selectRow(" + row + ") selectedRow=" + selectedRow + " " + dbgNameFull + " " + dbgUniqueID + cons);
	 
    styleRow(selectedRow, false);
    styleRow(row, true);

    	selectedRow = row;
 //   Person.get().displayItem(item);
    	SocialGraphExplorer.get().displayPersonDetail(person);
  	}

  private void styleRow(int row, boolean selected) {
    if (row != -1) {
      if (selected) {
        table.getRowFormatter().addStyleName(row + 1, "mail-SelectedRow");
      } else {
        table.getRowFormatter().removeStyleName(row + 1, "mail-SelectedRow");
      }
    }
  }
  private void markRowDisabled(int row, boolean selected) {
	    if (row != -1) {
	      if (selected) {
	        table.getRowFormatter().addStyleName(row + 1, "mail-DisabledRow");
	      } else {
	        table.getRowFormatter().removeStyleName(row + 1, "mail-DisabledRow");
	      }
	    }
	  }
  
  
  
  
  /*
   * External call to set state.
   * !@#$ We can optimise this later by keeping old anchor persons
   */
  public void updatePersonListExtern(String stringRep, boolean isRewind) {
	  SocialGraphExplorer.get().showInstantStatus("updatePersonListExtern(" + stringRep + ")", true);
	  
	  long  oldID = this.state.anchorUniqueID;
	  int	oldStartIndex = this.state.startIndex;
	  
	  CanonicalState newState = new CanonicalState(stringRep);
	  this.state = newState; // This case is just like startup
	  _needs2ndCacheCall = true;    // Exactly like startup
	 	 
	  //handleReturn() will call updateAnchor()
	  updatePersonList_("updatePersonListExtern(" + stringRep +") from " + oldID + ":" + oldStartIndex, isRewind);
  }
  private void updatePersonList(String dbgMsg) {
	  updatePersonList_(dbgMsg, false);
  }
  
  /*
   * Update the person list from the client cache
   * Returns through cacheCallbackUpdateList.handleReturn()
   * 
   *  The state of the UI is completely specified by this.state.anchorUniqueID and this.startIndex (and possibly 
   * incomplete fetches)
   * !@#$  Ugly stateChanged and !visibleFetched serve near identical roles. Use one or other ??
   * @param dbgMsg - for debugging
	* @param isRewind - is called from browser fwd and bck buttons?
    */
  	private void updatePersonList_(String dbgMsg, boolean isRewind) {
	  
		  if (!statesEqual(this.state, this.oldState))
			  this.state.visibleFetched = false;
		  
		  // Web history handling
		//  if (this.state.anchorUniqueID > 0 && !isRewind) {
			  setHistory(this.state.getAsString(), isRewind, this._needs2ndCacheCall);
		//  }
		  
		  SocialGraphExplorer.get().showInstantStatus("updatePersonList(" + dbgMsg +  ", " + !this.state.anchorFetched + ", " + !this.state.visibleFetched + ", " + isRewind + ")");
		 
		  long[][] fetchList = null;
		 
		  // Anchor changed so fetch a new anchor
		  // The callback will call again after this with anchorFetched set true ^&*
		  if (!this.state.anchorFetched) {
			  this.state.visibleFetched = false;
			  fetchList = Interval.getAnchorAndConnectionsIDs(this.state, null, VISIBLE_PERSONS_COUNT-1);
		  }
		  // Anchor is correct so fetch the full list
		  else if (!this.state.visibleFetched) {
			  this.uniqueIDsList = Interval.getAnchorAndConnectionsIDs(this.state, this.getAnchor().getConnectionIDs(), VISIBLE_PERSONS_COUNT-1);
			  fetchList = this.uniqueIDsList;
		  }
		   	 
		  // Fetch data from server
		  // Disable navigation buttons while fetching data to give client cache a single state
		  // cacheCallbackUpdateList() will call redrawUII() to re-enable the buttons after the server fetch
		  if (!this.state.anchorFetched || !this.state.visibleFetched) {
			  disableNavigation();
			  personClientCache.updateCacheAndGetVisible(fetchList, this.cacheCallbackUpdateList);
		  }

  	}
  	
  	private String  _lastHistoryItem = "";
	private boolean _savedFor2ndCall = false;
  	private boolean _savedIsRewind = false;
  	private void setHistory(String historyItem, boolean isRewind, boolean needs2ndCacheCall) {
  		if (needs2ndCacheCall) { 		// This is the first part of a 2 part call. 
  			_savedIsRewind = isRewind;	// Record state and wait
  			_savedFor2ndCall = true;
  		}
  		else {
  			if (_savedFor2ndCall) {  // Pop the saved state
  				isRewind = _savedIsRewind;
  				_savedFor2ndCall = false;
  			}
  			if (!_lastHistoryItem.equals(historyItem) && !isRewind && this.state.anchorUniqueID > 0) {
  				SocialGraphExplorer.get().showInstantStatus("History.newItem(" + historyItem + ")", true);
  				History.newItem(historyItem, false); // See http://google-web-toolkit.googlecode.com/svn/javadoc/1.6/com/google/gwt/user/client/History.html#newItem(java.lang.String, boolean)
  				_lastHistoryItem = historyItem;
  			}
  			
  		}
  	}
  
  	private boolean statesEqual(CanonicalState s1, CanonicalState s2) {
  		return 
	  		s1.startIndex == s2.startIndex &&
	  		s1.anchorUniqueID == s2.anchorUniqueID &&
	  		s1.anchorFetched == s2.anchorFetched;
  	}
  	
 
  	/*
     * Get person cache entry for row
     * @param row - row number on current screen
     * 	row 0 is always anchor
     * 	row i (i >0) is from anchor's connections
     * @return person cache entry for row or null if there is no cache entry 
     */
  	private PersonClient getPersonForRow(int row) {
  		assert(0 <= row && row < VISIBLE_PERSONS_COUNT);
  		PersonClient person = null;
  		
  		if (row == 0) {
  			person = this.theAnchor;
  		}
  		else if (row-1 < this.visiblePersons.size()) {	
  			person = visiblePersons.get(row-1);
  		}
		return person;
  	}
  	 
    
  	private static String squeeze(String sIn, int maxLen) {
  		String sOut = null;
  		if (sIn != null && sIn.length() > 0) {
  			int n1 = Math.max(maxLen, 0);
  			n1 = Math.min(n1, sIn.length());
  			sOut = sIn.substring(0, n1);
  		}
  		return sOut;
  	}
  	
  	private void redrawUI() {
  	//	SocialGraphExplorer.get().showInstantStatus("redrawUI()");
  	// Show the selected persons.
    	PersonClient person = null;
    		    	
        for (int i = 0; i < VISIBLE_PERSONS_COUNT; ++i) {
        	markRowDisabled(i, false);
        	person = getPersonForRow(i);
        	if (person != null) {
        		int numConnections = (person.getConnectionIDs() != null) ? person.getConnectionIDs().size() : 0;
    		   	table.setText(i+1 , 0, squeeze(person.getNameFull(), 20) + " - " + (i+ this.state.startIndex+1) + ",  " 
	        			+ person.getWhence() + ",  " 
	        			+ (person.getIsChildConnectionInProgress() ? "in progress" : "..") + ","
	        			+ (person.getHtmlPage() != null ? person.getHtmlPage().length()/1024 : 0) + "kb, " 
	        			+ person.getFetchDuration() + " sec, "
	        			+ person.getFetchDurationFull() + " sec"
	        			);
	        	table.setText(i+1 , 1, squeeze(person.getDescription(), 60) + " - " + numConnections);
	        	table.setText(i+1 , 2, person.getLocation() + " - " + person.getLiUniqueID());
        	}
        	else {
        		// Clear any remaining slots.
	        	table.setHTML(i+1 , 0, "&nbsp;");
	        	table.setHTML(i+1 , 1, "&nbsp;");
	        	table.setHTML(i+1 , 2, "&nbsp;");
	        }
        }
        // Select the first row if none is selected.
        if (selectedRow == -1) {
        	selectRow(0);
        }
    
        int count = getPersonCount();
  	  	// Update the older/newer buttons & label.
        //  int count = getPersonCount();
  	  	int max = this.state.startIndex + VISIBLE_PERSONS_COUNT;
  	  	if (max > count) {
  	  		max = count;
  	  	}

  	  	newerButton.setVisible(this.state.startIndex != 0);
  	  	newestButton.setVisible(this.state.startIndex > VISIBLE_PERSONS_COUNT);
  	  	olderButton.setVisible(this.state.startIndex + VISIBLE_PERSONS_COUNT < count);
  	  	oldestButton.setVisible(this.state.startIndex + 2* VISIBLE_PERSONS_COUNT < count);
  	  	countLabel.setText("" + (this.state.startIndex + 1) + " - " + max + " of " + count);
  	  	backButton.setVisible(isOldAnchors());
  	  	this.isNavigationDisabled = false;
      
  	  // Update the status message.
  	  // !!@#$
	}
  	
  	private void disableNavigation() {
  	//	SocialGraphExplorer.get().showInstantStatus("disableNavigation()");
  		this.isNavigationDisabled = true;
  		newerButton.setVisible(false);
  	  	newestButton.setVisible(false);
  	  	olderButton.setVisible(false);
  	  	oldestButton.setVisible(false);
  	  	backButton.setVisible(false);
  	  	
  	   for (int i = 1; i < VISIBLE_PERSONS_COUNT; ++i) {
  		   markRowDisabled(i, true);
  	   }
  	}
  	
  	private final CacheCallbackUpdateList cacheCallbackUpdateList = new CacheCallbackUpdateList();
  /*
   * Refresh screen after reading data
   * 	- data = one screen of entries
   * 	- startEntry = index into server list of entries
   * 	- numEntries = number of server entries
   * 	- incomplete = data from server is incomplete
   */
  	private class CacheCallbackUpdateList implements GetPersonsFromCacheCallback {
  		@Override
  		public void handleReturn(PersonClientCacheEntry[][] entries, 
  								 String description) {
  			
  			int dbgNumAnchors = entries[PersonClientCache.CACHE_LEVEL_ANCHOR].length;
  			int dbgNumVisible = entries[PersonClientCache.CACHE_LEVEL_VISIBLE].length;
  			SocialGraphExplorer.get().showInstantStatus(
  					 "handleReturn(" + description + ", " + dbgNumAnchors + ", " + dbgNumVisible+ ", " + state.anchorFetched+")", true);
  			SocialGraphExplorer.get().log("handleReturn", 
  	 					"[" + PersonClientCache.CACHE_LEVEL_ANCHOR +  ", " + PersonClientCache.CACHE_LEVEL_VISIBLE + "]: " 
  	 					+ PersonClientCache.getIdListForEntries(entries[PersonClientCache.CACHE_LEVEL_ANCHOR]) + ", "
  	 					+ PersonClientCache.getIdListForEntries(entries[PersonClientCache.CACHE_LEVEL_VISIBLE]),
  	 					false);    	
  			// Update anchorUniqueID in case there have been fetches from database
  			// How to handle missing data?? !@#$
	    	// More persons to fetch? Happens when last fetch was to update anchor  ^&*
	    	// This only gets called <=2 times since we set 2nd set of IDs to null here
  	
	    	if (!state.anchorFetched) {
	       		PersonClientCache.myAssert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR] != null);
	       		PersonClientCache.myAssert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR].length > 0);
  				PersonClientCacheEntry newAnchorEntry = entries[PersonClientCache.CACHE_LEVEL_ANCHOR][0];
  				PersonClient newAnchor = newAnchorEntry.getPerson();
  				PersonClientCache.myAssert(newAnchor != null);
  				updateAnchor(newAnchor, true);
 			}
	    	else {
	    		// A fetch with a valid anchor yields a valid visible state
	    		state.visibleFetched = true; 
	    		updateVisiblePersonsFromCache(entries[PersonClientCache.CACHE_LEVEL_VISIBLE]); 
	    		oldState = new CanonicalState(state);
	    	}
	    	
	    	redrawUI();
	    
	    	if (_needs2ndCacheCall) {
	    		_needs2ndCacheCall = false;
	    		updatePersonList("2ndCacheCall"); // call server a 2nd time
	    	}
		}	
	}
  
 	
}

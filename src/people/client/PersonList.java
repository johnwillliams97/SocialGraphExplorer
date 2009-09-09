package people.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
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
	// Total number of entries on a screen
	static final int VISIBLE_PERSONS_COUNT = OurConfiguration.VISIBLE_PERSONS_COUNT;  
	// Number of connection on a screen
	static final int CONNECTIONS_PER_SCREEN = VISIBLE_PERSONS_COUNT - 1;

	
	/*
	 * This UI is a list. First item in the list is the 'anchor'.
	 * The following items are indexes in anchor's connections list
	 * anchorUniqueID, startIndex and selectedRow completely describe the state of this clss; 
	 */
	
	private CanonicalState state = new CanonicalState();
	private CanonicalState oldState = new CanonicalState();;  // For tracking changes !@#$ is this needed?
	
	// When a person is first fetched from the cache, a 2nd call will be required to 
	// fetch all that persons connections.
	private boolean _needs2ndCacheCall = true; 
	// This also requires an extra UI state
	private CanonicalState _2ndState = new CanonicalState();
	
	// Row selected in the current frame. !@#$ Move this to CanonicalState
	private int	selectedRow = -1; 
	
	// Unique IDs of all persons tracked in this class
	private long[][] uniqueIDsList = null;
	
	// Visible persons data. This gets copied from the client cache.
	private PersonClient theAnchor = null;
	private List<PersonClient> visiblePersons = new ArrayList<PersonClient>();;
	
	
	// Widgets
	private HTML countLabel  = new HTML();
	private HTML lowerButton = new HTML("<a href='javascript:;'>&lt;</a>", true);
	private HTML higherButton = new HTML("<a href='javascript:;'>&gt;</a>", true);
	private HTML lowestButton = new HTML("<a href='javascript:;'>&lt;&lt;</a>", true);
	private HTML highestButton = new HTML("<a href='javascript:;'>&gt;&gt;</a>", true);

	private FlexTable table = new FlexTable();
	private HorizontalPanel navBar = new HorizontalPanel();
	private HorizontalPanel navBar1 = new HorizontalPanel();
	private HorizontalPanel navBar2 = new HorizontalPanel();

	private boolean isNavigationDisabled = false;

	static int debug_num_calls = 0; // !@#$ Why is PersonList() getting called multiple times???
	
	private static int[] _cacheLevelSize = null;
  	/*
  	 * Set up the list UI
  	 * @param urlStringRep - String representation of URL
  	 */
	public PersonList(CanonicalState canonicalState) {
		// GWT sometimes calls this twice in hosted mode.
		++debug_num_calls;
		if (debug_num_calls > 1) {
			Misc.myAssert(debug_num_calls <= 1, "PersonList - debug_num_calls <= ");
		}
		
		// Setup the cache
		_cacheLevelSize = new int[PersonClientCache.CACHE_LEVEL_NUMBER_LEVELS];
		_cacheLevelSize[PersonClientCache.CACHE_LEVEL_ANCHOR] = 1;
		_cacheLevelSize[PersonClientCache.CACHE_LEVEL_VISIBLE] = CONNECTIONS_PER_SCREEN;
		_cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK1] =  4 * CONNECTIONS_PER_SCREEN;
		_cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK2] =  4 * CONNECTIONS_PER_SCREEN;
		_cacheLevelSize[PersonClientCache.CACHE_LEVEL_RECENT] = OurConfiguration.CACHE_SIZE_LRU;
		personClientCache = new PersonClientCache(_cacheLevelSize);
	
		// Setup the table UI.
		table.setCellSpacing(0);
		table.setCellPadding(0);
		table.setWidth("100%");

		// Hook up events.
		table.addClickHandler(this);
		lowerButton.addClickHandler(this);
		higherButton.addClickHandler(this);
		lowestButton.addClickHandler(this);
		highestButton.addClickHandler(this);

		// Create the 'navigation' bar at the upper-right.
		HorizontalPanel innerNavBar = new HorizontalPanel();
		innerNavBar.add(countLabel);
		innerNavBar.add(lowestButton);
		innerNavBar.add(lowerButton);
		innerNavBar.add(higherButton);
		innerNavBar.add(highestButton);
	
		navBar.setStyleName("mail-ListNavBar");
		navBar.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
		navBar.add(innerNavBar);
		navBar.setWidth("100%");
		
		// All this navBar1 stuff is to get consistent styling across the row
		HorizontalPanel innerNavBar1 = new HorizontalPanel();
	//	innerNavBar1.add(dummyLabel1);
		navBar1.setStyleName("mail-ListNavBar");
		navBar1.add(innerNavBar1);
		navBar1.setWidth("100%");
		
		// All this navBar2 stuff is to get consistent styling across the row
		HorizontalPanel innerNavBar2 = new HorizontalPanel();
	//	innerNavBar2.add(dummyLabel2);
		navBar2.setStyleName("mail-ListNavBar");
		navBar2.add(innerNavBar2);
		navBar2.setWidth("100%");

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
       	updatePersonListExtern(canonicalState, false);  
	}

	/*
	 * Bring UI to a known state. This is used as the initial state.
	 */
	private void resetState() {
		theAnchor = null;
		visiblePersons = new ArrayList<PersonClient>();
		_needs2ndCacheCall = true;
		state = new CanonicalState();
		_2ndState = new CanonicalState();
	}
	
	@Override
		protected void onLoad() { // !@#$
	 // 
	}
  /**
   * Initialise the table so that it contains enough rows for a full page of
   * persons. 
   */
	private void initTable() {
       table.setWidget(0, 0, navBar);
       table.setWidget(0, 1, navBar1); // Gives consistent formatting across the whole row
       table.setWidget(0, 2, navBar2); // Gives consistent formatting across the whole row
       
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
	    if (sender == higherButton) {
	    //	SocialGraphExplorer.get().showInstantStatus("higherButton");
	      // Move forward a page.
	      this.state.startIndex += CONNECTIONS_PER_SCREEN;
	      int maxIndex = Interval.getMaxIndex(getAnchorConnectionIDs(), CONNECTIONS_PER_SCREEN);
	      if (this.state.startIndex > maxIndex) {
	        this.state.startIndex = maxIndex;
	      } else {
	        styleRow(selectedRow, false);
	        selectedRow = -1;
	        updatePersonList("higherButton");
	      }
	    } 
	    else if (sender == lowerButton) {
	    //	SocialGraphExplorer.get().showInstantStatus("lowerButton");
	      // Move back a page.
	      this.state.startIndex -= CONNECTIONS_PER_SCREEN;
	      if (this.state.startIndex < 0) {
	        this.state.startIndex = 0;
	      } else {
	        styleRow(selectedRow, false);
	        selectedRow = -1;
	        updatePersonList("lowerButton");
	      }
	    } 
	    else if (sender == highestButton) {
	    	//SocialGraphExplorer.get().showInstantStatus("highestButton");
		    // Move to end.
	        this.state.startIndex = Interval.getMaxIndex(getAnchorConnectionIDs(), CONNECTIONS_PER_SCREEN);
		    styleRow(selectedRow, false);
		    selectedRow = -1;
		    updatePersonList("highestButton");
		}
		else if (sender == lowestButton) {
			//SocialGraphExplorer.get().showInstantStatus("lowestButton");
		    // Move to start.
		    this.state.startIndex = 0;
		    styleRow(selectedRow, false);
		    selectedRow = -1;
		    updatePersonList("lowestButton");
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
	             	updatePersonListExtern(newState, false);
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
  	private List<Long> getAnchorConnectionIDs() {
  		List<Long> connectionIDs = null;
  		PersonClient person = getAnchor();
  		if (person != null && person != PersonClient.MAGIC_PERSON_CLIENT_1) {
  			connectionIDs = person.getConnectionIDs();
  		}
  		return connectionIDs;
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
			if (!this.state.anchorFetched)	{
				SocialGraphExplorer.get().showInstantStatus("updateAnchor(" + this.oldState.anchorUniqueID + " => " 
						+ newAnchor.getLiUniqueID() + "," + saveOldAnchor + ")");
				this.theAnchor = newAnchor;
				this.state.anchorUniqueID = newAnchor.getLiUniqueID();
				this.state.anchorFetched = true;
				this.state.visibleFetched = false;   // Visible list is invalid because anchor has changed
				oldState.anchorUniqueID = state.anchorUniqueID;  // !@#$ could do better!
			}
		} 
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
			int numConnections = Math.min(conIDs.size(), CONNECTIONS_PER_SCREEN);
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
   * Resets state
   	*/
  	public void updatePersonListExtern(CanonicalState canonicalState, boolean isRewind) {
	   
  		SocialGraphExplorer.get().showInstantStatus("updatePersonListExtern(" + canonicalState.anchorUniqueID + ":" + canonicalState.startIndex + ")", true);
	  
  		long oldID = this.state.anchorUniqueID;
  		int  oldStartIndex = this.state.startIndex;
		
  		// Reset state to start up
  		resetState();
  		
  		// Apply new state over reset state
  		state = new CanonicalState(canonicalState);
  		// Initially we get only anchor so we had better set indexes to 0
  		state.startIndex = -1;  // !@#$ Testing that state doesn't get used when there is a 2nd call: this._needs2ndCacheCall == true
  		// Eventually we get the full state
		_2ndState = new CanonicalState(canonicalState); 
		
		//handleReturn() will call updateAnchor()
  		updatePersonList_("updatePersonListExtern(" + canonicalState.anchorUniqueID + ":" + canonicalState.startIndex  +") from " + oldID + ":" + oldStartIndex, isRewind);
  	}
  
  	/*
   * Internal call to set state.
   * Does not reset state
   	*/
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
  		
    	// Validate indexes
    	if (theAnchor != null) {
    		int maxIndex = Interval.getMaxIndex(getAnchorConnectionIDs(), CONNECTIONS_PER_SCREEN);
    		state.startIndex = Math.max(state.startIndex, 0);
    		state.startIndex = Math.min(state.startIndex, maxIndex);
    	}
    	
    	PersonClient.debugValidate(this.theAnchor);
		if (!statesEqual(this.state, this.oldState)) {
			this.state.visibleFetched = false;
		}
		
		// Use a real uniqueID instead of a pseudo ID.
		CanonicalState saveState = new CanonicalState(this.state);
		if (saveState.anchorUniqueID == PersonClient.MAGIC_PERSON_CLIENT_1_UNIQUE_ID && theAnchor != null)
			saveState.anchorUniqueID = theAnchor.getLiUniqueID();
			
	  	// Save in web history
	  	saveStateInHistory(saveState, isRewind, this._needs2ndCacheCall);
	
	  	SocialGraphExplorer.get().showInstantStatus("updatePersonList(" + dbgMsg +  ", " + !this.state.anchorFetched + ", " + !this.state.visibleFetched + ", " + isRewind + ")");
	 
	  	long[][] fetchList = null;
	 
	  	// Anchor changed so fetch a new anchor
	  	// The callback will call again after this with anchorFetched set true ^&*
	  	if (!this.state.anchorFetched) {
	  		// Instrumentation
	  		printPersonList(this.state, null, CONNECTIONS_PER_SCREEN);
	
	  		this.state.visibleFetched = false;
	  		fetchList = Interval.getAnchorAndConnectionsIDs(this.state, null, CONNECTIONS_PER_SCREEN, _cacheLevelSize);
	  	}
	  	// Anchor is correct so fetch the full list
	  	else if (!this.state.visibleFetched) {
	  		// Instrumentation
	  		if (!this._needs2ndCacheCall)
	  			printPersonList(this.state, this.getAnchor().getConnectionIDs(), CONNECTIONS_PER_SCREEN);
	  		
	  		this.uniqueIDsList = Interval.getAnchorAndConnectionsIDs(this.state, this.getAnchor().getConnectionIDs(), CONNECTIONS_PER_SCREEN, _cacheLevelSize);
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
  	
    // Last history item saved. Track this to avoid duplicated items in history
  	private String  _lastHistoryItem = "";
  	// Indicates that a 2nd call to client cache will be needed
	private boolean _savedFor2ndCall = false;
	// Save isRewind arg in saveStateInHistory()
  	private boolean _savedIsRewind = false;
  	
   	/*
  	 * Save a state in the history list
  	 * @param historyItem - state to be saved
  	 * @param isRewind - true if this function is called in response to a browser arrow
  	 * @param needs2ndCacheCall - true if 2 cache calls will be needed (1 to fetch list of 
  	 *                             + 1 to fetch persons with those IDs
  	 */
  	private void saveStateInHistory(CanonicalState canonicalState, boolean isRewind, boolean needs2ndCacheCall) {
  		SystemState systemState = new SystemState(canonicalState.anchorUniqueID, canonicalState.startIndex);
  		String historyItem = systemState.getAsString();
  		
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
  				//SocialGraphExplorer.get().showInstantStatus("History.newItem(" + historyItem + ")", true);
  				History.newItem(historyItem, false); // See http://google-web-toolkit.googlecode.com/svn/javadoc/1.6/com/google/gwt/user/client/History.html#newItem(java.lang.String, boolean)
  				_lastHistoryItem = historyItem;
  				
  				updateWindowTitle(historyItem);
  			}
  		}
  	}
  	
  	// Holds the initial window title
	private  String _baseTitle = null;
	/*
	 * Update the window title so that old states can be looked up in the browser history list
	 * @param historyItem - URL pattern for current UI canonical state
	 */
  	private void updateWindowTitle(String historyItem)  {
  	// Do the title
		if (_baseTitle == null)
			_baseTitle = Window.getTitle();
		String title = (_baseTitle != null) ? _baseTitle : "???";
		if (theAnchor != null && theAnchor.getNameFull() != null)
			title +=  " :: " + theAnchor.getNameFull();
		title += " :: " + historyItem;
		Window.setTitle(title);
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
  		Misc.myAssert(0 <= row && row < VISIBLE_PERSONS_COUNT, "getPersonForRow - 0 <= row && row < VISIBLE_PERSONS_COUNT");
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
        		int index = 0; // Anchor
        		if (i > 0)
        			index = this.state.startIndex + (i-1) + 1; // Show indexes as being 1-offset
        		//String url = "http://www.linkedin.com/profile?viewProfile=&key=" + person.getLiUniqueID();
	        	//String link = "<a href='" + url + "'>" + squeeze(person.getNameFull(), 20) + "</a>";
        		String link = squeeze(person.getNameFull(), 20);
    		   	table.setText(i+1 , 0, link + " - " + index + ",  " 
	        			+ person.getWhence() + ",  " 
	        			+ (person.getIsChildConnectionInProgress() ? "in progress" : "..") + ","
	        			+ (person.getHtmlPage() != null ? person.getHtmlPage().length()/1024 : 0) + "kb, " 
	        			+ person.getFetchDuration() + " sec, "
	        			+ person.getFetchDurationFull() + " sec"
	        			+ ", level " + person.getCacheLevel()
	        			);
	        	table.setText(i+1 , 1, squeeze(person.getDescription(), 80) + " - " + numConnections);
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
    
        List<Long> connectionIDs = getAnchorConnectionIDs();
        int maxIndex = Interval.getMaxIndex(connectionIDs, CONNECTIONS_PER_SCREEN);
        int count = connectionIDs != null ? connectionIDs.size() : 0;
  	   	int max = Math.min(this.state.startIndex +  CONNECTIONS_PER_SCREEN, count);
     	
  	   	// Update the older/newer buttons & label.
  	  	lowerButton.setVisible(this.state.startIndex > 0);
  	  	lowestButton.setVisible(this.state.startIndex > CONNECTIONS_PER_SCREEN);
  	  	higherButton.setVisible(this.state.startIndex < maxIndex);
  	  	highestButton.setVisible(this.state.startIndex +  CONNECTIONS_PER_SCREEN < maxIndex);
  	  	countLabel.setText("" + (this.state.startIndex + 1) + " - " + max + " of " + count);
  	  	this.isNavigationDisabled = false;
	}
  	
  	private void disableNavigation() {
  	//	SocialGraphExplorer.get().showInstantStatus("disableNavigation()");
  		this.isNavigationDisabled = true;
  		lowerButton.setVisible(false);
  	  	lowestButton.setVisible(false);
  	  	higherButton.setVisible(false);
  	  	highestButton.setVisible(false);
 	  	
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
  					 "handleReturn(" + description + ", " + dbgNumAnchors + ", " + dbgNumVisible+ ", " + state.anchorFetched+")", false);
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
	       		Misc.myAssert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR] != null, "handleReturn - entries[PersonClientCache.CACHE_LEVEL_ANCHOR] != nul ");
	       		Misc.myAssert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR].length > 0, "handleReturn - entries[PersonClientCache.CACHE_LEVEL_ANCHOR].length > 0");
  				PersonClientCacheEntry newAnchorEntry = entries[PersonClientCache.CACHE_LEVEL_ANCHOR][0];
  				PersonClient newAnchor = newAnchorEntry.getPerson();
  				Misc.myAssert(newAnchor != null, "handleReturn - newAnchor != nul");
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
	    		state.startIndex = _2ndState.startIndex;
	    		updatePersonList("2ndCacheCall"); // call server a 2nd time
	    	}
		}	
  	}
  	
  	/*
  	 *  Instrumentation. Prints current visible and 1-click entries
  	 *  @param state - canonical state of UI     
     *  @param connectionIDs - list of IDs to predict caching for 
     *  @param rowsPerScreen - entries per screen of data
  	 */
	static private void printPersonList(
			CanonicalState state, 
			List<Long> connectionIDs,
			int rowsPerScreen) {
		int numConnections = connectionIDs != null ? connectionIDs.size() : 0;
		Misc.myAssert(state.startIndex < numConnections || state.startIndex == 0, "printPersonList - tate.startIndex < numConnections || state.startIndex == 0");
		String msg = "" + numConnections + ": " + state.anchorUniqueID + ", ";
		int i0 = Math.max(0, state.startIndex - rowsPerScreen);
		int i1 = state.startIndex;
		int i2 = Math.min(numConnections, state.startIndex + rowsPerScreen);
		int i3 = Math.min(numConnections, state.startIndex + 2*rowsPerScreen);
		msg += makeIDsString(connectionIDs, i0, i1) + ", ";
		msg += makeIDsString(connectionIDs, i1, i2) + ", ";
		msg += makeIDsString(connectionIDs, i2, i3) ;
		
		SocialGraphExplorer.get().log("printPersonList", msg, true);
		System.err.println("printPersonList: " + msg);
	}
	
	static private String makeIDsString(List<Long> connectionIDs, int i0, int i1) {
		String msg = "" + (i1-i0) + ":[";
		if (connectionIDs != null) {
			Misc.myAssert(0 <= i0 && i0 <= i1 && i1 <= connectionIDs.size(), "makeIDsString - 0 <= i0 && i0 <= i1 && i1 <= " + connectionIDs.size());
			for (int i = i0; i < i1; ++i) {
				msg += connectionIDs.get(i) + ",";
			}
		}
		msg += "]";
		return msg;
	}
 	
}

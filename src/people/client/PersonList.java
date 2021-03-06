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
	private final PersonClientCache _personClientCache;
	
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
	
	private CanonicalState _state = new CanonicalState();
	private CanonicalState _oldState = new CanonicalState();;  // For tracking changes !@#$ is this needed?
	
	// When a person is first fetched from the cache, a 2nd call will be required to 
	// fetch all that persons connections.
	private boolean _needs2ndCacheCall = true; 
	// This also requires an extra UI state
	private CanonicalState _2ndState = new CanonicalState();
	
	// Row selected in the current frame. !@#$ Move this to CanonicalState
	private int	_selectedRow = -1; 
	
	// Unique IDs of all persons tracked in this class
	private long[][] _uniqueIDsList = null;
	
	// Visible persons data. This gets copied from the client cache.
	private PersonClient _theAnchor = null;
	private List<PersonClient> _visiblePersons = new ArrayList<PersonClient>();;
	
	
	// Widgets
	private HTML _countLabel  = new HTML();
	private HTML _lowerButton = new HTML("<a href='javascript:;'>&lt;</a>", true);
	private HTML _higherButton = new HTML("<a href='javascript:;'>&gt;</a>", true);
	private HTML _lowestButton = new HTML("<a href='javascript:;'>&lt;&lt;</a>", true);
	private HTML _highestButton = new HTML("<a href='javascript:;'>&gt;&gt;</a>", true);

	private FlexTable _table = new FlexTable();
	private HorizontalPanel _navBar = new HorizontalPanel();
	private HorizontalPanel _navBar1 = new HorizontalPanel();
	private HorizontalPanel _navBar2 = new HorizontalPanel();

	private boolean _isNavigationDisabled = false;

	static int debug_num_calls = 0; // !@#$ Why is PersonList() getting called multiple times???
	
	private static int[] _cacheLevelSize = null;
	
	// For dummy testing mode
	// tells server to attach a payload of this size to each person record when running in ADD_FAKE_HTML mode
	private int _payloadBytes = -1;

  	/*
  	 * Set up the list UI
  	 * @param canonicalState - Cannonical state read from  URL
  	 * @param maxServerCallsPerRequest - Dynamic (overridable from URL) version of OurConfiguration.MAX_SERVER_CALLS_PER_REQUEST
  	 * @param maxServerCallsInProgress - Dynamic (overridable from URL) version of OurConfiguration.MAX_REQUESTS_IN_PROGRESS
  	 * @param maxServerCallsInProgress - Dynamic (overridable from URL) version of OurConfiguration.MAX_SERVER_CALLS_IN_PROGRESS
  	 * @param payloadBytes - tells server to attach a payload of this size to each person record when running in ADD_FAKE_HTML mode
  	 */
	public PersonList(CanonicalState canonicalState, 
				      int maxServerCallsPerRequest,
				      int maxRequestsInProgress,
				      int maxServerCallsInProgress,
				      int payloadBytes) {
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
		_personClientCache = new PersonClientCache(_cacheLevelSize, maxServerCallsPerRequest, maxRequestsInProgress, maxServerCallsInProgress, payloadBytes);
	
		_payloadBytes = payloadBytes;
		
		// Setup the table UI.
		_table.setCellSpacing(0);
		_table.setCellPadding(0);
		_table.setWidth("100%");

		// Hook up events.
		_table.addClickHandler(this);
		_lowerButton.addClickHandler(this);
		_higherButton.addClickHandler(this);
		_lowestButton.addClickHandler(this);
		_highestButton.addClickHandler(this);

		// Create the 'navigation' bar at the upper-right.
		HorizontalPanel innerNavBar = new HorizontalPanel();
		innerNavBar.add(_countLabel);
		innerNavBar.add(_lowestButton);
		innerNavBar.add(_lowerButton);
		innerNavBar.add(_higherButton);
		innerNavBar.add(_highestButton);
	
		_navBar.setStyleName("mail-ListNavBar");
		_navBar.setHorizontalAlignment(HorizontalPanel.ALIGN_LEFT);
		_navBar.add(innerNavBar);
		_navBar.setWidth("100%");
		
		// All this navBar1 stuff is to get consistent styling across the row
		HorizontalPanel innerNavBar1 = new HorizontalPanel();
	//	innerNavBar1.add(dummyLabel1);
		_navBar1.setStyleName("mail-ListNavBar");
		_navBar1.add(innerNavBar1);
		_navBar1.setWidth("100%");
		
		// All this navBar2 stuff is to get consistent styling across the row
		HorizontalPanel innerNavBar2 = new HorizontalPanel();
	//	innerNavBar2.add(dummyLabel2);
		_navBar2.setStyleName("mail-ListNavBar");
		_navBar2.add(innerNavBar2);
		_navBar2.setWidth("100%");

		initWidget(_table);
		setStyleName("mail-List");

    	initTable();
    
    	/*
    	 *  Initialise data. !@#$ Need to call twice. See above.
    	 *  	First call fetches a server person for the anchor
    	 *  	Second call fetches anchor's connections
    	 * 	_state.anchorFetched is used to do this, after the cache returns from updatePersonList() in
    	 * 	cacheCallbackUpdateList.handleReturn()
    	 * 
    	 */
       	updatePersonListExtern(canonicalState, false);  
	}

	/*
	 * Bring UI to a known state. This is used as the initial state.
	 */
	private void resetState() {
		_theAnchor = null;
		_visiblePersons = new ArrayList<PersonClient>();
		_needs2ndCacheCall = true;
		_state = new CanonicalState();
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
       _table.setWidget(0, 0, _navBar);
       _table.setWidget(0, 1, _navBar1); // Gives consistent formatting across the whole row
       _table.setWidget(0, 2, _navBar2); // Gives consistent formatting across the whole row
       
       _table.getRowFormatter().setStyleName(0, "mail-ListHeader");

       // Initialise the rest of the rows.
       for (int i = 0; i < VISIBLE_PERSONS_COUNT; ++i) {
	      _table.setText(i + 1, 0, "");
	      _table.setText(i + 1, 1, "");
	      _table.setText(i + 1, 2, "");
	      _table.getCellFormatter().setWordWrap(i + 1, 0, false);
	      _table.getCellFormatter().setWordWrap(i + 1, 1, false);
	      _table.getCellFormatter().setWordWrap(i + 1, 2, false);
	      _table.getFlexCellFormatter().setColSpan(i + 1, 2, 2);
       }
	}

	/*
	 * (non-Javadoc)
	 * @see com.google.gwt.event.dom.client.ClickHandler#onClick(com.google.gwt.event.dom.client.ClickEvent)
	 * Respond to UI events
	 * 
	 * The state of the UI is completely specified by _state.anchorUniqueID and this.startIndex (and possibly 
	 * incomplete fetches)
	 */
	@Override
	public void onClick(ClickEvent event) {
		//SocialGraphExplorer.get().showInstantStatus("onClick()");
	    Object sender = event.getSource();
	    if (sender == _higherButton) {
	    //	SocialGraphExplorer.get().showInstantStatus("higherButton");
	      // Move forward a page.
	      _state.startIndex += CONNECTIONS_PER_SCREEN;
	      int maxIndex = Interval.getMaxIndex(getAnchorConnectionIDs(), CONNECTIONS_PER_SCREEN);
	      if (_state.startIndex > maxIndex) {
	        _state.startIndex = maxIndex;
	      } else {
	        styleRow(_selectedRow, false);
	        resetSelectedRow();
	        updatePersonList("higherButton");
	      }
	    } 
	    else if (sender == _lowerButton) {
	    //	SocialGraphExplorer.get().showInstantStatus("lowerButton");
	      // Move back a page.
	      _state.startIndex -= CONNECTIONS_PER_SCREEN;
	      if (_state.startIndex < 0) {
	        _state.startIndex = 0;
	      } else {
	        styleRow(_selectedRow, false);
	        resetSelectedRow();
	        updatePersonList("lowerButton");
	      }
	    } 
	    else if (sender == _highestButton) {
	    	//SocialGraphExplorer.get().showInstantStatus("highestButton");
		    // Move to end.
	        _state.startIndex = Interval.getMaxIndex(getAnchorConnectionIDs(), CONNECTIONS_PER_SCREEN);
		    styleRow(_selectedRow, false);
		    resetSelectedRow();
		    updatePersonList("highestButton");
		}
		else if (sender == _lowestButton) {
			//SocialGraphExplorer.get().showInstantStatus("lowestButton");
		    // Move to start.
		    _state.startIndex = 0;
		    styleRow(_selectedRow, false);
		    _selectedRow = -1;
		    updatePersonList("lowestButton");
		} 
	    else if (sender == _table) {
	    	//  SocialGraphExplorer.get().showInstantStatus("table ");
	      // Select the row that was clicked (-1 to account for header row).
	      Cell cell = _table.getCellForEvent(event);
	      if (cell != null && !_isNavigationDisabled ) {
	    	int row = cell.getRowIndex() - 1;
	        if (row >= 0 && getPersonForRow(row) != null) {
	        	SocialGraphExplorer.get().showInstantStatus("table row " + row);
	        	if (_selectedRow != row) {
	        		selectRow(row);		// Change selected row
	        	}
	        	else  {
	        		styleRow(_selectedRow, false);
	        		_selectedRow = -1;
	        		CanonicalState newState = new CanonicalState(getPersonForRow(row).getUniqueID(), 0);
	             	updatePersonListExtern(newState, false);
	         	}
	        }
	      }
	    }
	}
	
	private void resetSelectedRow() {
	//	_selectedRow = -1;
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
  		return _theAnchor;
  	}
  	/*
     * Update anchor person to the new one fetched from the cache
    */
  	private void updateAnchor(PersonClient newAnchor, boolean saveOldAnchor) {
  		if (newAnchor != null && !newAnchor.isMagicPerson()) { 	// Update to a real person?
			if (!_state.anchorFetched)	{
				SocialGraphExplorer.get().showInstantStatus("updateAnchor(" + _oldState.anchorUniqueID + " => " 
						+ newAnchor.getUniqueID() + "," + saveOldAnchor + ")");
				_theAnchor = newAnchor;
				_state.anchorUniqueID = newAnchor.getUniqueID();
				_state.anchorFetched = true;
				_state.visibleFetched = false;   // Visible list is invalid because anchor has changed
				_oldState.anchorUniqueID = _state.anchorUniqueID;  // !@#$ could do better!
			}
		} 
  	}
  	
    	
  
	private void updateVisiblePersonsFromCache(PersonClientCacheEntry[] entries)  {
  		if (entries != null && entries.length > 0) {
	  		long[] ids = _uniqueIDsList[PersonClientCache.CACHE_LEVEL_VISIBLE];
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
	  		_visiblePersons = newVisiblePersons;
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
			
	    styleRow(_selectedRow, false);
	    styleRow(row, true);
	   	_selectedRow = row;
	   	SocialGraphExplorer.get().displayPersonDetail(person);
  	}

  	private void styleRow(int row, boolean selected) {
  		if (row != -1) {
  			if (selected) {
  				_table.getRowFormatter().addStyleName(row + 1, "mail-SelectedRow");
  			} 
  			else {
  				_table.getRowFormatter().removeStyleName(row + 1, "mail-SelectedRow");
  			}
  		}
  	}
  	
  	private void markRowDisabled(int row, boolean selected) {
	    if (row != -1) {
	      if (selected) {
	        _table.getRowFormatter().addStyleName(row + 1, "mail-DisabledRow");
	      } else {
	        _table.getRowFormatter().removeStyleName(row + 1, "mail-DisabledRow");
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
	  
  		long oldID = _state.anchorUniqueID;
  		int  oldStartIndex = _state.startIndex;
		
  		// Reset state to start up
  		resetState();
  		
  		// Apply new state over reset state
  		_state = new CanonicalState(canonicalState);
  		// Initially we get only anchor so we had better set indexes to 0
  		_state.startIndex = -1;  // !@#$ Testing that state doesn't get used when there is a 2nd call: this._needs2ndCacheCall == true
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
   *  The state of the UI is completely specified by _state.anchorUniqueID and this.startIndex (and possibly 
   * incomplete fetches)
   * !@#$  Ugly stateChanged and !visibleFetched serve near identical roles. Use one or other ??
   * @param dbgMsg - for debugging
	* @param isRewind - is called from browser fwd and bck buttons?
    */
    private void updatePersonList_(String dbgMsg, boolean isRewind) {
  		
    	// Validate indexes
    	if (_theAnchor != null) {
    		int maxIndex = Interval.getMaxIndex(getAnchorConnectionIDs(), CONNECTIONS_PER_SCREEN);
    		_state.startIndex = Math.max(_state.startIndex, 0);
    		_state.startIndex = Math.min(_state.startIndex, maxIndex);
    		Misc.myAssert(maxIndex >= 0, "maxIndex >= 0");
    		Misc.myAssert(_state.startIndex >= 0, "_state.startIndex >= 0");
    	}
    	
    	PersonClient.debugValidate(_theAnchor);
		if (!statesEqual(_state, _oldState)) {
			_state.visibleFetched = false;
		}
		
		// Use a real uniqueID instead of a pseudo ID.
		CanonicalState saveState = new CanonicalState(_state);
		if (saveState.anchorUniqueID == PersonClient.MAGIC_PERSON_CLIENT_1_UNIQUE_ID && _theAnchor != null)
			saveState.anchorUniqueID = _theAnchor.getUniqueID();
			
	  	// Save in web history
	  	saveStateInHistory(saveState, isRewind, _needs2ndCacheCall);
	
	  	SocialGraphExplorer.get().showInstantStatus("updatePersonList(" + dbgMsg +  ", " + !_state.anchorFetched + ", " + !_state.visibleFetched + ", " + isRewind + ")");
	 
	  	long[][] fetchList = null;
	 
	  	// Anchor changed so fetch a new anchor
	  	// The callback will call again after this with anchorFetched set true ^&*
	  	if (!_state.anchorFetched) {
	  		// Instrumentation
	  		printPersonList(_state, null, CONNECTIONS_PER_SCREEN);
	
	  		_state.visibleFetched = false;
	  		fetchList = Interval.getAnchorAndConnectionsIDs(_state, null, CONNECTIONS_PER_SCREEN, _cacheLevelSize);
	  	}
	  	// Anchor is correct so fetch the full list
	  	else if (!_state.visibleFetched) {
	  		// Instrumentation
	  		if (!_needs2ndCacheCall)
	  			printPersonList(_state, getAnchor().getConnectionIDs(), CONNECTIONS_PER_SCREEN);
	  		
	  		_uniqueIDsList = Interval.getAnchorAndConnectionsIDs(_state, getAnchor().getConnectionIDs(), CONNECTIONS_PER_SCREEN, _cacheLevelSize);
	  		fetchList = _uniqueIDsList;
	  	}
	   	 
	  	// Fetch data from server
	  	// Disable navigation buttons while fetching data to give client cache a single state
	  	// cacheCallbackUpdateList() will call redrawUII() to re-enable the buttons after the server fetch
	  	if (!_state.anchorFetched || !_state.visibleFetched) {
	  		disableNavigation();
	  		_personClientCache.updateCacheAndGetVisible(fetchList, cacheCallbackUpdateList);
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
  		SystemState systemState = new SystemState(canonicalState.anchorUniqueID, canonicalState.startIndex, _payloadBytes);
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
  			if (!_lastHistoryItem.equals(historyItem) && !isRewind && _state.anchorUniqueID > 0) {
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
		if (_theAnchor != null && _theAnchor.getNameFull() != null)
			title +=  " :: " + _theAnchor.getNameFull();
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
  			person = _theAnchor;
  		}
  		else if (row-1 < _visiblePersons.size()) {	
  			person = _visiblePersons.get(row-1);
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
    	int highestRow = 0;	    	
        for (int i = 0; i < VISIBLE_PERSONS_COUNT; ++i) {
        	markRowDisabled(i, false);
        	person = getPersonForRow(i);
        	if (person != null) {
        		int numConnections = (person.getConnectionIDs() != null) ? person.getConnectionIDs().size() : 0;
        		int index = 0; // Anchor
        		if (i > 0)
        			index = _state.startIndex + (i-1) + 1; // Show indexes as being 1-offset
        		String link = squeeze(person.getNameFull(), 30);
        		String stats = "";
        		if (OurConfiguration.DEBUG_MODE) {
        			stats = person.getWhence() + ",  " 
        			+ Misc.showBytes(person.getHtmlPage() != null ? person.getHtmlPage().length() : 0) + ", " 
        			+ person.getFetchDuration() + " sec, "
        			+ person.getFetchDurationFull() + " sec"
        			+ ", level " + person.getCacheLevel();
        		}
        		
        		_table.setText(i+1 , 0, ""  + index + ": " + link + " (unique ID " + person.getUniqueID() + ") " + stats);
	        	_table.setText(i+1 , 1, squeeze(person.getDescription(), 80) + " with " + numConnections + " connections");
	            _table.setText(i+1 , 2, person.getLocation());
	            highestRow = i;
	    	}
        	else {
        		// Clear any remaining slots.
	        	_table.setHTML(i+1 , 0, "&nbsp;");
	        	_table.setHTML(i+1 , 1, "&nbsp;");
	        	_table.setHTML(i+1 , 2, "&nbsp;");
	        }
        }
        // Select the first row if none is selected.
        if (_selectedRow > highestRow) {
        	_selectedRow = highestRow;
        }
        if (_selectedRow < 0) {
        	_selectedRow = 0;
        }
       
        selectRow(_selectedRow);
    
        List<Long> connectionIDs = getAnchorConnectionIDs();
        int maxIndex = Interval.getMaxIndex(connectionIDs, CONNECTIONS_PER_SCREEN);
        int count = connectionIDs != null ? connectionIDs.size() : 0;
  	   	int max = Math.min(_state.startIndex +  CONNECTIONS_PER_SCREEN, count);
     	
  	   	// Update the older/newer buttons & label.
  	  	_lowerButton.setVisible(_state.startIndex > 0);
  	  	_lowestButton.setVisible(_state.startIndex > CONNECTIONS_PER_SCREEN);
  	  	_higherButton.setVisible(_state.startIndex < maxIndex);
  	  	_highestButton.setVisible(_state.startIndex +  CONNECTIONS_PER_SCREEN < maxIndex);
  	  	_countLabel.setText("" + (_state.startIndex + 1) + " - " + max + " of " + count);
  	  	_isNavigationDisabled = false;
	}
  	
  	private void disableNavigation() {
  	//	SocialGraphExplorer.get().showInstantStatus("disableNavigation()");
  		_isNavigationDisabled = true;
  		_lowerButton.setVisible(false);
  	  	_lowestButton.setVisible(false);
  	  	_higherButton.setVisible(false);
  	  	_highestButton.setVisible(false);
 	  	
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
  					 "handleReturn(" + description + ", " + dbgNumAnchors + ", " + dbgNumVisible+ ", " + _state.anchorFetched+")", false);
  			SocialGraphExplorer.get().log("handleReturn", 
  	 					"[" + PersonClientCache.CACHE_LEVEL_ANCHOR +  ", " + PersonClientCache.CACHE_LEVEL_VISIBLE + "]: " 
  	 					+ PersonClientCache.getIdListForEntries(entries[PersonClientCache.CACHE_LEVEL_ANCHOR]) + ", "
  	 					+ PersonClientCache.getIdListForEntries(entries[PersonClientCache.CACHE_LEVEL_VISIBLE]),
  	 					false);    	
  			// Update anchorUniqueID in case there have been fetches from database
  			// How to handle missing data?? !@#$
	    	// More persons to fetch? Happens when last fetch was to update anchor  ^&*
	    	// This only gets called <=2 times since we set 2nd set of IDs to null here
  	
	    	if (!_state.anchorFetched) {
	       		Misc.myAssert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR] != null, "handleReturn - entries[PersonClientCache.CACHE_LEVEL_ANCHOR] != nul ");
	       		Misc.myAssert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR].length > 0, "handleReturn - entries[PersonClientCache.CACHE_LEVEL_ANCHOR].length > 0");
  				PersonClientCacheEntry newAnchorEntry = entries[PersonClientCache.CACHE_LEVEL_ANCHOR][0];
  				PersonClient newAnchor = newAnchorEntry.getPerson();
  				Misc.myAssert(newAnchor != null, "handleReturn - newAnchor != nul");
  				updateAnchor(newAnchor, true);
 			}
	    	else {
	    		// A fetch with a valid anchor yields a valid visible state
	    		_state.visibleFetched = true; 
	    		updateVisiblePersonsFromCache(entries[PersonClientCache.CACHE_LEVEL_VISIBLE]); 
	    		_oldState = new CanonicalState(_state);
	    	}
	    	
	    	redrawUI();
	    
	    	if (_needs2ndCacheCall) {
	    		_needs2ndCacheCall = false;
	    		_state.startIndex = _2ndState.startIndex;
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

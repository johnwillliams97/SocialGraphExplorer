package people.client;


import java.util.ArrayList;
import java.util.List;
import people.client.PersonClientCache.GetPersonsFromCacheCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTMLTable.Cell;


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
	
	private boolean _firstFetch = true;
	/*
	 * This UI is a list. First item in the list is the 'anchor'.
	 * The following items are indexes in anchor's connections list
	 * anchorUniqueID, startIndex and selectedRow completely describe the state of this clss; 
	 */
	private class CanonicalState {
		public long  anchorUniqueID;   // The ID of the anchor person
		public int   startIndex;
		public boolean anchorFetched;
		public boolean visibleFetched; 
		public CanonicalState(CanonicalState s1) {
			this.anchorUniqueID = s1.anchorUniqueID;
			this.startIndex = s1.startIndex;
			this.anchorFetched = s1.anchorFetched;
			this.visibleFetched = s1.visibleFetched;
		}
		private void init() {
			this.anchorUniqueID = PersonClient.MAGIC_PERSON_CLIENT_1_UNIQUE_ID;   // The ID of the anchor person
			this.startIndex = 0;
			this.anchorFetched = false;
			this.visibleFetched = false;
		}
		public CanonicalState() {
			init();
		}
		private static final String SEPARATOR = ",";
		public String getAsString() {
			return "" + this.anchorUniqueID + SEPARATOR + this.startIndex;
		}
		CanonicalState(String stringRep) {
			init();
			String[] parts = stringRep.split(SEPARATOR);
			if (parts.length > 0)
				this.anchorUniqueID = Long.parseLong(parts[0]);
			if (parts.length > 1)
				this.startIndex = Integer.parseInt(parts[1]);
		}
		
	}
	
	private CanonicalState state = new CanonicalState();
	private CanonicalState oldState = new CanonicalState();;  // For tracking changes
	
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


  	/*
  	 * Set up the list UI
  	 */
	public PersonList() {
	  
		
		// Setup the cache
		int[] cacheLevelSize = new int[PersonClientCache.CACHE_LEVEL_NUMBER_LEVELS];
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_ANCHOR] = 1 + 2*ANCHOR_HISTORY_COUNT;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_VISIBLE] = VISIBLE_PERSONS_COUNT - 1;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK1] = (4 + VISIBLE_PERSONS_COUNT - 1) * VISIBLE_PERSONS_COUNT;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_CLICK2] = (2) * VISIBLE_PERSONS_COUNT;
		cacheLevelSize[PersonClientCache.CACHE_LEVEL_RECENT] = 100;
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
    	
    	updatePersonList("Constructor");
   
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
	        		this.state.startIndex = 0;
	        		long oldID = this.state.anchorUniqueID;
	        		updateAnchor(getPersonForRow(row), true); // Make this person the anchor
	             	selectedRow = -1;
	        		updatePersonList("new anchor = " + oldID + " => " + this.state.anchorUniqueID);
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
			if (this.state.anchorUniqueID != newAnchor.getLiUniqueID()) {	// Anything changed?
				SocialGraphExplorer.get().showInstantStatus("updateAnchor(" + this.state.anchorUniqueID + " => " 
						+ newAnchor.getLiUniqueID() + "," + saveOldAnchor + ")");
				if (this.theAnchor != null && !this.theAnchor.isMagicPerson() && saveOldAnchor)
					oldAnchors.push(this.theAnchor);
				SocialGraphExplorer.get().log("oldAnchors", oldAnchors.getState());
				this.theAnchor = newAnchor;
				this.state.anchorUniqueID = newAnchor.getLiUniqueID();
				this.state.anchorFetched = true;
				this.state.visibleFetched = false;   // Visible list is invalid because anchor has changed
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
  /*
   * n0..n1-1
   */
  	static class Interval {
	  int n0;
	  int n1;
	  public Interval(int n0, int n1,  int max) {
		  assert(n1 >= n0);
		  assert(max > 0);
		  if (n0 < 0)
			 n0 = 0;
		  if (n1 > max)
			 n1 = max;
		  if (n1 < n0)
			 n1 = n0;
		 
		 this.n0 = n0;
		 this.n1 = n1;
		 assert(n1 >= n0);
	  }
	  public int length() {
		  return (n1 - n0);
	  }
  	};
  	static private Interval remove(Interval removee, Interval remover) {
	  // Fully enclosed => empty
	  if (remover.n0 <= removee.n0 && removee.n1 <= remover.n1)
		  removee.n1 = removee.n0;
	  // left end is intersected => move right
	  else if (remover.n0 <= removee.n0 && removee.n0 <= remover.n1)
		  removee.n0 = remover.n1;
	  // right end is intersected move left
	  else if (remover.n0 <= removee.n1 && removee.n1 <= remover.n1)
		  removee.n1 = remover.n0;
	  assert(removee.n1 >= removee.n0);
	  if (removee.n1 < removee.n0)
		  removee.n1 = removee.n0;
	  assert(removee.n1 >= removee.n0);
	  return removee;
  	}
  	static int[] intervalsToArray(Interval[] intervals) {
	  System.err.println("intervalsToArray(" + intervals.length + " intervals)");
	  int len = 0;
	  for (int i = 0; i < intervals.length; i++) {
		  len += intervals[i].length();
	  }
	  System.err.println("  array will have " + len + " elements)");
	  int[] out = null;
	  if (len > 0) {
		  out = new int[len];
	 	  int count = 0;
		  for (int i = 0; i < intervals.length; i++) {
			  for (int j = intervals[i].n0; j < intervals[i].n1; j++) {
				  out[count++] = j;
			  }
		  }
	  }
	  return out;
  	}
  	static long[] indexesToUniqueIDs(List<Long> connections, int[] indexes) {
	  System.err.println("indexesToUniqueIDs(" + (indexes != null ? indexes.length : 0) + " indexes)");
	  long[] uniqueIDs = null;
	  if (indexes != null) {
		  assert(indexes.length <= connections.size());
		  uniqueIDs = new long[indexes.length];
		  for (int i = 0; i < indexes.length; ++i) {
			  uniqueIDs[i] = connections.get(indexes[i]);
		  }
		  System.err.println("  " + uniqueIDs.length + " IDs");
	  }
	  return uniqueIDs;
  	}
  
  /*
   * Get the set of visible and about to be visible IDs for the current UI state
   * @return - list of IDs
   * 
   * Note. This depends on anchorUniqueID, startIndex from this UI state and 
   *       the anchor person having been fetched from the cache
   */
  static private long[][] getAnchorAndConnectionsIDs(CanonicalState state_, PersonClient anchor) {
	  
	  long[][] uniqueIDsList = new long[PersonClientCache.CACHE_LEVEL_SETTABLE_LEVELS][];
	  uniqueIDsList[PersonClientCache.CACHE_LEVEL_ANCHOR] = new long[1];
	  uniqueIDsList[PersonClientCache.CACHE_LEVEL_ANCHOR][0] = state_.anchorUniqueID;
	  
	  int numConnections = 0;
	//  PersonClient anchor = getAnchor();
	  
	  if (anchor != null) {
		  List<Long> connectionIDs = anchor.getConnectionIDs();
		  if (connectionIDs != null) {
			  numConnections = anchor.getConnectionIDs().size();
		  
			  /* Anchor = anchorUniqueID
			   * Visible =  connections[<N>..<N>+VISIBLE_PERSONS_COUNT-2] N=startIndex
			   * Visible+1= connections[<N>..<N>+VISIBLE_PERSONS_COUNT-1] 
			   * 			N=0, N=numConnections-VISIBLE_PERSONS_COUNT,
			   * 			N=startIndex+VISIBLE_PERSONS_COUNT
			   * 			N=startIndex-VISIBLE_PERSONS_COUNT
			   * Visible+2= connections[<N>..<N>+VISIBLE_PERSONS_COUNT-1] 
			   * 			N=startIndex+2*VISIBLE_PERSONS_COUNT
			   * 			N=startIndex-2*VISIBLE_PERSONS_COUNT
			   */
			  int screenFull = VISIBLE_PERSONS_COUNT-1;
			  
			  /* 
			   * Raw regions 
			   * 	v=visible
			   * 	c=start+end = 1 click
			   * 	d=+-1 screen = 1 click
			   * 	e=+-2 screens = 2 clicks
			   */
			  Interval v  = new Interval(state_.startIndex, state_.startIndex + screenFull, numConnections);
			  Interval c1 = new Interval(0, screenFull, numConnections); 
			  Interval c2 = new Interval(numConnections - screenFull, numConnections, numConnections); 
			  Interval d1 = new Interval(state_.startIndex - 2*screenFull, state_.startIndex - 1*screenFull, numConnections); 
			  Interval d2 = new Interval(state_.startIndex + 1*screenFull, state_.startIndex + 2*screenFull, numConnections); 
			  Interval e1 = new Interval(state_.startIndex - 3*screenFull, state_.startIndex - 2*screenFull, numConnections); 
			  Interval e2 = new Interval(state_.startIndex + 2*screenFull, state_.startIndex + 3*screenFull, numConnections); 
			  
			  // Remove intersections
			  Interval[] intervals = { v, c1, c2, d1, d2, e1, e2 };
			  for (int i = 1; i < intervals.length; ++i) {
				  for (int j = 0; j < i; ++j) {
					 intervals[i] = remove(intervals[i], intervals[j]) ;
				  }
			  }
			  System.err.println("<<< " + intervals.length + " intervals");
			  Interval[] intervalVisible = {v};
			  Interval[] intervalClick1  = { c1, c2, d1, d2 };
			  Interval[] intervalClick2  = { e1, e2 };
			  int[] idxVisible = intervalsToArray(intervalVisible);
			  int[] idxClick1  = intervalsToArray(intervalClick1);
			  int[] idxClick2  = intervalsToArray(intervalClick2);
			  uniqueIDsList[PersonClientCache.CACHE_LEVEL_VISIBLE]= indexesToUniqueIDs(connectionIDs, idxVisible);
			  uniqueIDsList[PersonClientCache.CACHE_LEVEL_CLICK1] = indexesToUniqueIDs(connectionIDs, idxClick1);
			  uniqueIDsList[PersonClientCache.CACHE_LEVEL_CLICK2] = indexesToUniqueIDs(connectionIDs, idxClick2);
		  }
	  } 
	  return uniqueIDsList;
  }
  
  public void updatePersonListExtern(String stringRep) {
	  this.state = new CanonicalState(stringRep);
	  updatePersonList("updatePersonListExtern(" + stringRep +")");
  }
  /*
   * Update the person list from the client cache
   * Returns through cacheCallbackUpdateList.handleReturn()
   * 
   *  The state of the UI is completely specified by this.state.anchorUniqueID and this.startIndex (and possibly 
	 * incomplete fetches)
	 * !@#$  Ugly stateChanged and !visibleFetched serve near identical roles. Use one or other ??
   */
  	private void updatePersonList(String dbgMsg) {
	  
		  if (!statesEqual(this.state, this.oldState))
			  this.state.visibleFetched = false;
		  
		  // Web history handling
		  if (this.state.anchorUniqueID > 0) {
			  History.newItem(this.state.getAsString()); 
		  }
		  
		  SocialGraphExplorer.get().showInstantStatus("updatePersonList(" + dbgMsg +  ", " + !this.state.anchorFetched + ", " + !this.state.visibleFetched + ")");
		 
		  long[][] fetchList = null;
		 
		  // Anchor changed so fetch a new anchor
		  // The callback will call again after this with anchorFetched set true ^&*
		  if (!this.state.anchorFetched) {
			  this.state.visibleFetched = false;
			  fetchList = getAnchorAndConnectionsIDs(this.state, null);
		  }
		  // Anchor is correct so fetch the full list
		  else if (!this.state.visibleFetched) {
			  this.uniqueIDsList = getAnchorAndConnectionsIDs(this.state, this.getAnchor());
			  fetchList = this.uniqueIDsList;
			//  this.oldState = this.state;  now done in this.cacheCallbackUpdateList
		  }
		   	 
		  // Fetch data from server
		  // Disable navigation buttons while fetching data to give client cache a single state
		  // cacheCallbackUpdateList() will call redrawUII() to re-enable the buttons after the server fetch
		  if (!this.state.anchorFetched || !this.state.visibleFetched) {
			  disableNavigation();
			  personClientCache.updateCacheAndGetVisible(fetchList, null, this.cacheCallbackUpdateList);
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
        	person = getPersonForRow(i);
        	if (person != null) {
        		int numConnections = (person.getConnectionIDs() != null) ? person.getConnectionIDs().size() : 0;
    		   	table.setText(i+1 , 0, squeeze(person.getNameFull(), 40) + " - " + i + ",  " 
	        			+ person.getWhence() + ",  " 
	        			+ (person.getIsChildConnectionInProgress() ? "in progress" : "..") + ","
	        			+ (person.getHtmlPage() != null ? person.getHtmlPage().length()/1024 : 0) + "kb, " 
	        			+ person.getFetchDuration() + " sec"
	        			);
	        	table.setText(i+1 , 1, squeeze(person.getDescription(), 40) + " - " + numConnections);
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
  					 "handleReturn(" + description + ", " + dbgNumAnchors + ", " + dbgNumVisible+ ")");
	    		    	
  			// Update anchorUniqueID in case there have been fetches from database
  			// How to handle missing data?? !@#$
	    	// More persons to fetch? Happens when last fetch was to update anchor  ^&*
	    	// This only gets called <=2 times since we set 2nd set of IDs to null here
  	//		boolean needSecondUpdate = false;
	    	if (!state.anchorFetched) {
	    		state.anchorFetched = true;
	    		oldState.anchorUniqueID = state.anchorUniqueID; 
	    		state.visibleFetched = false;     // A change of anchor invalidates the visible state
	    			    	
	    		assert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR]!= null);
	    		assert(entries[PersonClientCache.CACHE_LEVEL_ANCHOR].length > 0);
  				PersonClientCacheEntry newAnchorEntry = entries[PersonClientCache.CACHE_LEVEL_ANCHOR][0];
  				PersonClient newAnchor = newAnchorEntry.getPerson();
  				assert(newAnchor != null);
  				updateAnchor(newAnchor, true);
 			}
	    	else {
	    		// A fetch with a valid anchor yields a valid visible state
	    		state.visibleFetched = true; 
	    		updateVisiblePersonsFromCache(entries[PersonClientCache.CACHE_LEVEL_VISIBLE]); 
	    		oldState = new CanonicalState(state);
	    	}
	    	
	    	redrawUI();
	    
	    	if (_firstFetch) {
  				updatePersonList("follow-up"); // call server a 2nd time
  				_firstFetch = false;
	    	}
		}	
	}
  
 	
}

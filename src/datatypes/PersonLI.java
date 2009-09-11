package datatypes;
/*
 * http://www.youtube.com/watch?v=tx5gdoNpcZM&feature=channel 
 */

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import com.google.appengine.api.datastore.Text;
import cache.CacheTrait;
import people.client.Misc;
import people.client.OurConfiguration;
import people.client.Statistics;
import people.client.PersonTrait;
import db.PMF;

@PersistenceCapable(identityType=IdentityType.APPLICATION)
public class PersonLI  implements PersistentPersonTrait, CacheTrait {
	
	private static final long serialVersionUID = -6983889227350201932L;

	private static final Logger logger = Logger.getLogger(PersonLI.class.getName());

	public static final long DEFAULT_PERSON_RECORD_UNIQUEID = OurConfiguration.AUTHOR_UNIQUEID;

	@SuppressWarnings("unused")
	@PrimaryKey
	@Persistent(valueStrategy=IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	private long revision = 0L;

	@Persistent
	private long liUniqueID = -1L;			
  											

/*
 * The public interface. Keys I care about.	
 */
	@Persistent
	private long rank;						// My ranking of the person
  
	@Persistent
 	private String nameFull;				
	
	@Persistent
 	private String nameFirst;				//  First name, for searching
  
	@Persistent								//  Last name, for searching
	private String nameLast;
  
	@Persistent
	private String location;
	
	@Persistent
	private String employer;
	

	
	@Persistent
	private String description;
	

	/*
	 * Encode booleans in flags. egg
	 * 		isPotentialEmployer
	 * 		isInMyNetwork
	 * 		isInAustralia
	 */
	@SuppressWarnings("unused")
	@Persistent
	private long flags;							
	
	/*
	 * High level description of a person. This is cached in each connection entry
	 * to save database reads
	 */
	class PersonLISummary {
		long 		uniqueID;
		ReadState  	readState = ReadState.RECORD_READ_MINIMAL;
		long 		sortIdx;
		
		PersonLISummary(long uniqueID) {
			this.uniqueID = uniqueID;
		}
		PersonLISummary(PersonLI person) {
			this.uniqueID  = person.getUniqueID();
			this.readState = person.getReadState();
			this.sortIdx   = person.getNumConnections();
		}
	};
	
	@SuppressWarnings("unused")
	@Persistent
	private List<PersonLISummary> connections;					// Summaries of connections
	
	@Persistent
	private List<Long> connectionIDs;					// liUniqueIDs of connections
	
	@Persistent
	private long		numConnectionIDs;				// Cache connectionIDs.size() here so we can search on it
	
	@SuppressWarnings("unused")
	@Persistent
	private List<Long> queriesForThis;					// List of indexes into the queries that returned this record
	
		
	@Persistent
//	private long	recordReadProgress = 0;
	/*
	 *  childConnectionProgress[CHILDREN_CONNECTIONS]
	 *  For each type
	 *  	> 0 = progress e.g. #pages read
	 *  	-1 = not started
	 *      -2 = complete
	 */
	public static final int NUM_PROGRESS_TYPES = 40;
	
	public static final int CHILDREN_CONNECTIONS = 0;
		
	public static final int RECORD_READ_PROGRESS = NUM_PROGRESS_TYPES - 1;
	
	public static final long PROGRESS_NOT_STARTED = -1L;
	public static final long PROGRESS_COMPLETED = 0L; // -2L;
	
	@Persistent
	private long[] 	childConnectionProgress = new long[NUM_PROGRESS_TYPES]; 
	
	/*  Set isChildConnectionInProgress to true when any connection is in progress
	 * 	Search database for isChildConnectionInProgress == true
	 * 	Then search through each record's childConnectionProgress for details
	 */
	@SuppressWarnings("unused")
	@Persistent
	private boolean isChildConnectionInProgress = false; 
	  

	@Persistent								
	private Text htmlPage;					// Full html page for a person 
  
	@Persistent
	private Date whenReadFirst;				// When first read 
	
	@Persistent
	private Date whenReadLast;				// When last read 

	// Track sorting of connections. This needs to be optimised since sorting requires 
	// a DB access for every connection
	// !@#$ Long, not long, since it is missing from previous versions in DB
	@Persistent
	private Long numConnectionsSorted;
 
	MessageDigest connectionsMD5;
	
	//byte[] thedigest = md.digest(bytes);
	  public PersonLI(String nameFirst, 
			  		String nameLast, 
			  		long   liUniqueID,
			  		long   rank,
			  		String location,
			  		String description,
			  		List<Long> connectionIDs) {
		    this.nameFirst = nameFirst;
		    this.nameLast  = nameLast;
		    this.nameFull  = nameFirst + " " + nameLast;
		    this.liUniqueID = liUniqueID;
		    this.rank = rank;
		    this.location = location;
		    this.description = description;
		    this.connectionIDs = connectionIDs;
		 //   MessageDigest.getInstance("MD5");
	  }
	  /*
	   * http://leepoint.net/notes-java/oop/constructors/constructor.html
	   */
	  public PersonLI() {
		  	this(
		  			(String)null, 
		  			(String)null, 
			  		0L, 0L,
			  		(String)null,
			  		(String)null,
			  		(List<Long>)null);
	  }
	  /*
		 * (non-Javadoc)
		 * @see people.client.PersonTrait#isRealData()
		 * PersonLI data is real!
		 */
	  @Override
	  public boolean isRealData() {
		return true;
	  }
  	 /*
  	  * getters & setters
  	  */
	  public void setNameFirst(String nameFirst) {
		   this.nameFirst = nameFirst;
	  }
	  public String getNameFirst() {
		  return this.nameFirst;
	  }
	  
	  public void setNameLast(String nameLast) {
		   this.nameLast = nameLast;
	  }
	  public String getNameLast() {
		  return this.nameLast;
	  }
	  
	  @Override
	  public void setNameFull(String nameFull) {
		   this.nameFull = nameFull;
	  }
	  
	  @Override
	  public String getNameFull() {
		  return this.nameFull;
	  }
	  
	  @Override
	  public void setUniqueID(long liUniqueID) {
		  this.liUniqueID = liUniqueID;
	  }
	  
	  @Override
	  public long getUniqueID() {
		  return this.liUniqueID;
	  }
	
	  public long getNumConnections() { 
		  // Break numConnectionIDs caching because it got out of synch somehow !@#$
		  this.numConnectionIDs = (this.connectionIDs != null) ? this.connectionIDs.size() : 0L;
		  
		  return this.numConnectionIDs;
	  }
	  
	  @Override
	  public List<Long> getConnectionIDs() {
		  return connectionIDs;
	  }
	
	  @Override
	  public void setConnectionIDs(List<Long> connectionIDs) {
		  if (connectionIDs == null) {
			  this.connectionIDs = null;
		  }
		  else if (connectionIDs != this.connectionIDs) {
			  List<Long> tmp = new ArrayList<Long>(); // Shouldn't need a temp if connectionIDs != this.connectionIDs
			  tmp.addAll(connectionIDs);
			  this.connectionIDs = tmp;
		  }
		  this.numConnectionIDs = (this.connectionIDs != null) ? this.connectionIDs.size() : 0L;
	  }
	  public void addConnectionIDs(List<Long> connectionIDs) {
		  if (this.connectionIDs == null)
			  setConnectionIDs(connectionIDs);
		  else if (connectionIDs != null) {
			  this.connectionIDs.addAll(connectionIDs);
			  this.numConnectionIDs = (this.connectionIDs != null) ? this.connectionIDs.size() : 0L;
		  }
	  }
	  
	  public static final int MAX_DESCRIPTION = 400;

	 
	  @Override
	  public void setDescription(String description) {
		  if (description!= null && description.length() > MAX_DESCRIPTION) {
			 logger.info("description (b)[" + description.length()+ "] " + description);
		  }
		  else {
			  this.description = description;
		  }
	  }
	  
		@Override
		public String getEmployer() {
			return this.employer;
		}
		
		@Override
		public void setEmployer(String employer) {
			this.employer = employer;
		}
		
		@Override
		public void setLocation(String location) {
			this.location = location;
		}
		
		@Override
		public String getLocation() {
	  		return this.location;
	  	}
		
		@Override
		public String getHtmlPage() {
			return (this.htmlPage != null ? this.htmlPage.getValue() : null);
		}
		@Override
		public void setHtmlPage(String htmlPage) {
			if (htmlPage != null) {
				this.htmlPage = new Text(htmlPage);
			}
		}
		
	 
	/*
	 * Server & DB managment
	 *   
	 */
	  
	@Override
	public void setIsChildConnectionInProgress(boolean isChildConnectionInProgress) {
		this.isChildConnectionInProgress = isChildConnectionInProgress;	
	}
	
	@Override
	public boolean getIsChildConnectionInProgress() {
		// This flag is currently broken !@#$
		//return this.isChildConnectionInProgress;
		// so do it the hard (accurate) way
		boolean connectionInProgress = false;
		for (int connType = 0; connType < this.childConnectionProgress.length; connType++) {
			if (this.childConnectionProgress[connType] != PersonLI.PROGRESS_COMPLETED) {
				connectionInProgress = true;
				break;
			}
		}
		return connectionInProgress;
	}
	
	public void setChildConnectionProgress(int index, long progress) {
		  assert(0 <= index && index < getNumChildConnectionProgresses());
		  childConnectionProgress[index] = progress; 
		  if (progress != PROGRESS_COMPLETED)
			  isChildConnectionInProgress = true;			// Set the dirty flag!
	  }
	  public long getNumChildConnectionProgresses(){
		  return childConnectionProgress.length;      
	  }
	  public long getChildConnectionProgress(int index) {
		  assert(0 <= index && index < childConnectionProgress.length);
		  return childConnectionProgress[index];      
	  }
	  public String getChildConnectionProgressDescription(int index) {
		  String progDesc = "bad state!!";
		  long progress = getChildConnectionProgress(index);
		  if (progress == PROGRESS_NOT_STARTED)
			  progDesc = "Connections not read";
		  else if (progress == PROGRESS_COMPLETED)
			  progDesc = "Complete";
		  else if (progress > 0L)
			  progDesc = progress + " pages of connections read";
		  return progDesc;      
	  }
	  
	  
	
 
  	public long getRank() {
  		return rank;
  	}
  	
  	public Date getReadFirst() {
  		return this.whenReadFirst;
  	}
  	public Date getReadLast() {
  		return this.whenReadLast;
  	}
  
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
  	
    @Override
	public String getDescription() {
  		String descClean = decodeUrl(this.description);
  		return descClean;
  	}
  	  	
  	public boolean isSameAs(PersonLI person) {
  		return (this.getUniqueID() == person.getUniqueID());
  	}
  	/*
  	 * Returns all people with a given uniqueID.
  	 * There should be only one such person, since we should only store one record for 
  	 * each unique ID
  	 * http://code.google.com/appengine/docs/java/datastore/queriesandindexes.html
  	 * 
  	 * Called only by getPersonByUniqueId_()
  	 */
  	@SuppressWarnings("unchecked")
	private static List<PersonLI> getPersonsWithUniqueId(PersistenceManager pm, long uniqueID) {
  		logger.info("getPersonsWithUniqueId(" + uniqueID + ")");
  		List<PersonLI> results = null;
  		Query query = pm.newQuery(PersonLI.class);
  		query.setFilter("liUniqueID == liUniqueIDParam");
  		query.setOrdering("whenReadLast desc"); // If multiple records with liUniqueID then take most recent // was query.setOrdering("liUniqueID asc"); 
  		query.declareParameters("long liUniqueIDParam");
  		try {
  			results = (List<PersonLI>) query.execute(uniqueID);
  	        if (results != null) {
  	           	assert(results.size() <= 1);
  	        }
  	    } 
  		catch (Exception e) {
  			logger.warning("exception:" + e.getMessage());
  			Misc.reportException(e);
  		}
  		finally {
  		    query.closeAll();
  	    }
  	
  	    return results;
  	}
  	/*
  	 * Get the list of people with the specified list of IDs
  	 * This is an attempt at optimisation by grouping DB fetches
  	 * It may need some work.
  	 */
  	/*
  	@SuppressWarnings("unchecked")
	private static List<PersonLI> getPersonsWithUniqueIdList(List<Long> connectionIDs) {
  		if (connectionIDs == null)
  			return null;
  		List<PersonLI> persons = new ArrayList<PersonLI>();
 		PersistenceManager pm = PMF.getQueryPm(); 
  		Query query = pm.newQuery(PersonLI.class);
  		query.setFilter("liUniqueID == liUniqueIDParam");
  		query.setOrdering("liUniqueID ascending");
  		query.declareParameters("Long liUniqueIDParam");
  		try {
  			for (Long uniqueID: connectionIDs) {
  				List<PersonLI> results = (List<PersonLI>) query.execute(uniqueID);
	  	        if (results != null) {
	  	        	int rs = results.size();
	  	        	if (rs > 0) {
	  	        		assert(rs <= 1);
		  	        	PersonLI p = results.get(0);
		  	        	persons.add(p);
	  	        	}
	  	        }
  			}
  	    } 
  		catch (Exception e) {
  			Misc.reportException(e);
  		}
  		finally {
  	        query.closeAll();
  	        pm.close();
  	    }
  		if (persons.size() < 1)
  			persons = null;
  	    return persons;
  	}
  	*/
	private static PersonLI getPersonByUniqueId_(PersistenceManager pm, long uniqueID) {
  		PersonLI person = null;
  		List<PersonLI> results = getPersonsWithUniqueId(pm, uniqueID) ;
  		if (results != null) {
	  	    assert(results.size() <= 1);
	  	    if (results.size() >= 1){
	  	      	person = results.get(0);
	  	    } 
  		}
  		
  		String nameFull = person != null ? person.getNameFull() : "none";
  		
  		Statistics.getInstance().recordEvent("PersonLI.getPersonByUniqueId_(" + uniqueID + ") = " + nameFull);
  		
  		return person;
  	}
	
  	/*
  	 * Will a simple to = from work?
  	 */
	private static void copyFields(PersonLI from, PersonLI to) {
  		to.nameFirst = from.nameFirst;
  		to.nameLast  = from.nameLast;
  		to.nameFull  = from.nameFull;
  		to.liUniqueID = from.liUniqueID;
  
  		to.rank = from.rank;
	    to.location = from.location;
	    to.description = from.description;
	    if (from.connectionIDs == null)
	    	to.connectionIDs = null;
	    else {
	    	List<Long> tmp = new ArrayList<Long>();
	    	tmp.addAll(from.connectionIDs);
		    to.connectionIDs = tmp;
		//    assert(to.getNumConnections() == from.getNumConnections());
	    }
	    to.numConnectionsSorted = from.numConnectionsSorted;
	    to.numConnectionIDs = from.getNumConnections();
	    to.htmlPage = from.htmlPage;
	    to.childConnectionProgress = from.childConnectionProgress;
	    to.whenReadFirst = from.whenReadFirst;
	    to.whenReadLast = from.whenReadLast;
	    to.revision = from.revision + 1;
  	}
  	/*
  	 * http://code.google.com/appengine/docs/java/datastore/creatinggettinganddeletingdata.html
  	 */
  	public  void saveToDB() {
  		this.whenReadLast = Calendar.getInstance().getTime();
  		if (this.whenReadFirst == null)
  			this.whenReadFirst = this.whenReadLast;
  		
  		PersonLI p = null;
  		PersistenceManager pm = PMF.get().getPersistenceManager();;
  		try {	
  			p = getPersonByUniqueId_(pm, this.liUniqueID);
  			if (p != null) { 
  				// Person found in DB so update fields. 
  				// Person should get saved in close below
	  			assert(p.getUniqueID() == this.liUniqueID);
	  			copyFields(this, p); 
  			}
  		}
		catch (Exception e) { // !@#$ Could catch and finally be over both blocks?
  			Misc.reportException(e);
  		}
  		finally {
  			pm.close();
  		}
  	// Person was not in DB, so add them to db
  		if (p == null) { 
	  		pm = PMF.get().getPersistenceManager();;
	  		try {		
		  		pm.makePersistent(this);
	  		}
	  		catch (Exception e) {
	  			Misc.reportException(e);
	  		}
	  		finally {
	  			pm.close();
	  		}
  		}
   	}
  	
  	public  void saveToDB0() {
  		this.whenReadLast = Calendar.getInstance().getTime();
  		if (this.whenReadFirst == null)
  			this.whenReadFirst = this.whenReadLast;
  		
  		PersonLI p = null;
  		PersistenceManager pm = PMF.get().getPersistenceManager();;
  		try {	
  			p = getPersonByUniqueId_(pm, this.liUniqueID);
  		}
		catch (Exception e) {
  			Misc.reportException(e);
  		}
  		finally {
  			pm.close();
  		}
  		
  		pm = PMF.get().getPersistenceManager();;
  		try {		
	  		if (p != null) {
	  			assert(p.getUniqueID() == this.liUniqueID);
	  			copyFields(this, p); 
	  		//	pm.makePersistent(p); // !@#$ is this needed?
	  		}
	  		else {
	  			p = this;
	  			pm.makePersistent(p);
	  		}
   		}
  		catch (Exception e) {
  			Misc.reportException(e);
  		}
  		finally {
  			pm.close();
  		}
   	}
  
  	public static void insert(String nameFirst, 
						  		String nameLast, 
						  		long   liUniqueID,
						  		long   rank,
						  		String location,
						  		String description,
						  		List<Long> connectionIDs) {
  		PersonLI entry = new PersonLI( 
	    		nameFirst, 
		  		nameLast, 
		  		liUniqueID,
		  		rank,
		  		location,
		  		description,
		  		connectionIDs );
  	//	entry.save();
	    PersistenceManager pm = PMF.get().getPersistenceManager();
	    pm.makePersistent(entry);
	    pm.close();
  	}
  	
 	  	
	/*
	 * !Public interface to this module
	 * 
	 * Find person by their liUniqueID
	 * If no person found then return null
	 */
	public static PersonLI findInDBbyUniqueId(long liUniqueID) {
		Statistics.getInstance().recordEvent("PersonLI.findInDBbyUniqueId(" + liUniqueID + ")");
		PersistenceManager pm = PMF.get().getPersistenceManager();;
  		PersonLI p = null;
  		try {
  			p = getPersonByUniqueId_(pm, liUniqueID);
  			if (p != null) {
  				assert(p.getUniqueID() == liUniqueID);
   	  		}
  		} 
  		catch (Exception e) {
  			Misc.reportException(e);
  		}
  		finally {
   			pm.close();
  		}
   		return p;
	}
  	
	/*
	 * Derive readState from other data
	 * @see people.client.PersonLIPublic#getReadState()
	 */
	@Override
	public ReadState getReadState() {
		ReadState readState = ReadState.RECORD_READ_MINIMAL;
		if (this.nameFull != null) {
			readState = ReadState.RECORD_READ_BASIC;
			if (!getIsChildConnectionInProgress())
				readState = ReadState.RECORD_READ_FULL;
		}
		return readState;
	}
	@Override
	public void setReadState(ReadState readState) {
	//	does nothing since readState is synthesised from other state
	}
	public boolean isConnectionsSorted() {
		long n1 = (this.numConnectionsSorted != null) ? this.numConnectionsSorted : 0L;
		long n2 = this.getNumConnections();
		return (n1 == n2);
	}
	public void setConnectionsSorted(boolean doSet) {
		this.numConnectionsSorted = doSet ? this.getNumConnections() : 0L;
	}
	
	// !@#$ Hack for checking cache performance
	// This shows which cache the person was fetched from
	@NotPersistent
	String whence = null;
	@Override
	public void setWhence(String whence) {
		logger.info("setWhence("+whence+") - " + (this.nameFull != null ? this.nameFull : "not found"));
		this.whence = whence;
	}
	@Override
	public String getWhence() {
		return this.whence;
	}
	// !@#$ Hack for checking cache performance
	// This shows how long it took to fetch the person
	@NotPersistent
	double fetchDuration = 0.0;
	@Override
	public void setFetchDuration(double fetchDuration) {
		this.fetchDuration = fetchDuration;
	}
	@Override
	public double getFetchDuration() {
		return this.fetchDuration;
	}
	
	@Override
	public boolean isIncomplete() {
		boolean incomplete = getIsChildConnectionInProgress() || getHtmlPage() == null;
		long uniqueID = getUniqueID();
		String nameFull = getNameFull();
		String compl = incomplete ? "INCOMPLETE" : "complete";
		if (incomplete)
			logger.warning(uniqueID + ":" + nameFull + " - " + compl);
		return incomplete;
	}
	
	/*
	 * A Claytons Person for when there is no person
	 */
	public static final PersonLI NO_PERSON = new PersonLI(
			"Donald", 
	  		"Duck", 
	  		PersonTrait.NO_PERSON_UNIQUEID,
	  		0,
	  		"Disneyland",
	  		"A Duck in the Entertainment Business",
	  		null);

}

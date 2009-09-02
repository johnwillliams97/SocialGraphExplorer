package datatypes;
/*
 * http://www.youtube.com/watch?v=tx5gdoNpcZM&feature=channel 
 */
import java.io.Serializable;
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

import cache.CacheTrait;

import com.google.appengine.api.datastore.Text;
import people.client.Statistics;
import people.client.PersonPublic;
import db.PMF;

@PersistenceCapable(identityType=IdentityType.APPLICATION)
public class PersonLI  implements Serializable, PersonPublic, CacheTrait {
	
	//private static final long serialVersionUID = 1L;
//	private static final long serialVersionUID = 6745964760100019554L;
//	private static final long serialVersionUID = -3088974083721413636L;
//	private static final long serialVersionUID = 7683849060655852899L;
	private static final long serialVersionUID = -6983889227350201932L;

	 private static final Logger logger = Logger.getLogger(PersonLI.class.getName());
/*
	private String show() {
		String out = " ";
		out += this.getLiUniqueID() + ", ";
		out += this.getNameFull() + ", " ;
		out += this.getNumConnections();
		return out;
	}
*/
	@SuppressWarnings("unused")
	@PrimaryKey
	@Persistent(valueStrategy=IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	@Persistent
	private long revision = 0L;

	@Persistent
	private long liUniqueID = -1L;			// http://www.linkedin.com/profile?viewProfile=&key=258598
  											// liUniqueID = 258598, taken from URL

/*
 * The public interface. Keys I care about.	
 */
	@Persistent
	private long rank;						// My ranking of the person
  
	@Persistent
 	private String nameFull;				//  <title>LinkedIn: ILYA KAUSHANSKY</title> 
	
	@Persistent
 	private String nameFirst;				//  First name, for searching
  
	@Persistent								//  Last name, for searching
	private String nameLast;
  
	@Persistent
	private String location;
	
	@Persistent
	private String employer;
	
//	@Persistent
//	private long degreeOfSeparation = -1L;
	
	@Persistent
	private String description;
	
//	@Persistent
//	private String email;
	
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
			this.uniqueID  = person.getLiUniqueID();
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
	
	/*
	 * Internals
	 * 	State of partial searches
	 * 	LinkedIn page contents
	 * 
	 */
	
	
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
	public static final int CHILDREN_RECOMMENDATIONS = 1;
	public static final int CHILDREN_RECOMMENDED_BY = 2;
	public static final int CHILDREN_ALSO_VIEWED = 3;
	public static final int CHILDREN_WVMP = 4;

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
	private Text htmlPage;				// Full html page. Keep for later analysis?
  
	@Persistent
	private Date whenReadFirst;				// When first read from LI
	
	@Persistent
	private Date whenReadLast;				// When last read from LI

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
	  
	  public void setNameFull(String nameFull) {
		   this.nameFull = nameFull;
	  }
	  public String getNameFull() {
		  return this.nameFull;
	  }
	  
	  public void setLiUniqueID(long liUniqueID) {
		  this.liUniqueID = liUniqueID;
	  }
	  public long getLiUniqueID() {
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

	  public static final long DEFAULT_LI_UNIQUEID = 1960788L; // Me
	
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
				e.printStackTrace();
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
	public String getDescription() {
  		String descClean = decodeUrl(this.description);
  		return descClean;
  	}
  	  	
  	public boolean isSameAs(PersonLI person) {
  		return (this.getLiUniqueID() == person.getLiUniqueID());
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
  			//logger.warning("before");
  	        results = (List<PersonLI>) query.execute(uniqueID);
  	       // logger.warning("after");
  	        if (results != null) {
  	        //	logger.warning("People = " + results.size());
  	        //	for (PersonLI person: results) 
  	        //		logger.warning(person.show());
  	        	assert(results.size() <= 1);
  	        }
  	    } 
  		catch (Exception e) {
  			logger.warning("exception");
  			e.printStackTrace();
  		}
  		finally {
  		//	logger.warning("finally");
  	        query.closeAll();
  	    //    logger.warning("finally2");
  	    }
  	//	logger.warning("return");
  	    return results;
  	}
  	/*
  	 * Get the list of people with the specified list of IDs
  	 * This is an attempt at optimisation by grouping db fetches
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
  			e.printStackTrace();
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
	//	Statistics.getInstance().recordEvent("PersonLI.getPersonByUniqueId_(" + uniqueID + ")");
		//PersistenceManager pm = PMF.getQueryPm(); // PMF.get().getPersistenceManager();;
	//	logger.info("getPersonByUniqueId_(" + uniqueID + ")");
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
	 * Return all a person's connections
	 */
	/*
	public static List<PersonLI> getConnectionsForPerson(PersonLI person) {
		List<PersonLI> connections = getPersonsWithUniqueIdList(person.getConnectionIDs());
		return connections; 	
  	}
  	*/
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
	  			assert(p.getLiUniqueID() == this.liUniqueID);
	  			copyFields(this, p); 
  			}
  		}
		catch (Exception e) { // !@#$ Could catch and finally be over both blocks?
  			e.printStackTrace();
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
	  			e.printStackTrace();
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
  			e.printStackTrace();
  		}
  		finally {
  			pm.close();
  		}
  		
  		pm = PMF.get().getPersistenceManager();;
  		try {		
	  		if (p != null) {
	  			assert(p.getLiUniqueID() == this.liUniqueID);
	  			copyFields(this, p); 
	  		//	pm.makePersistent(p); // !@#$ is this needed?
	  		}
	  		else {
	  			p = this;
	  			pm.makePersistent(p);
	  		}
   		}
  		catch (Exception e) {
  			e.printStackTrace();
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
  	@SuppressWarnings("unchecked")
 	private static List<PersonLI> getEntries() {
	    PersistenceManager pm = PMF.get().getPersistenceManager();
	    Query query = pm.newQuery(PersonLI.class);
	    query.setOrdering("liUniqueID ASC");
	    List<PersonLI> entries = (List<PersonLI>) query.execute();
	    query.closeAll();
	    return entries;
  	}
  	*/
  	
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
  		//	logger.warning("after");
  			if (p != null) {
  		//		logger.warning("person = " + p.getNameFull());
  	  			assert(p.getLiUniqueID() == liUniqueID);
   	  		}
  		} 
  		catch (Exception e) {
  			e.printStackTrace();
  		}
  		finally {
  	//		logger.warning("final");
  			pm.close();
  		}
  //		logger.warning("return");
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
		boolean incomplete = getIsChildConnectionInProgress() || getHtmlPage() == null;// 
		long uniqueID = getLiUniqueID();
		String nameFull = getNameFull();
		String compl = incomplete ? "INCOMPLETE" : "complete";
		if (incomplete)
			logger.warning(uniqueID + ":" + nameFull + " - " + compl);
		return incomplete;
	}
}

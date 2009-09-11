package datatypes;
/*
 * http://www.youtube.com/watch?v=tx5gdoNpcZM&feature=channel 
 */


import java.util.ArrayList;

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
import people.client.Statistics;
import db.PMF;

@PersistenceCapable(identityType=IdentityType.APPLICATION)
public class PersonDummy  implements PersistentPersonTrait, CacheTrait {
	
	private static final long serialVersionUID = -8878621563703014847L;

	private static final Logger logger = Logger.getLogger(PersonDummy.class.getName());

	public static final long DEFAULT_PERSON_RECORD_UNIQUEID = 100L;

	@SuppressWarnings("unused")
	@PrimaryKey
	@Persistent(valueStrategy=IdGeneratorStrategy.IDENTITY)
	private Long id;
	
	
	@Persistent
	private long _uniqueID = -1L;			
  											

/*
 * The public interface. Keys that users care about.	
 */
  

	@Persistent
 	private String _nameFirst;				//  First name, for searching
  
	@Persistent							
	private String _nameLast;				//  Last name, for searching
  
	@Persistent
	private String _location;
	
	@Persistent
	private String _employer;
	
	@Persistent
	private String _description;
	
			
	@Persistent
	private List<Long> _connectionIDs;					// uniqueIDs of connections
	
	
	@Persistent								
	private Text _htmlPage;					// Full html page for a person 
  
	
	public PersonDummy(String nameFirst, 
			  		String nameLast, 
			  		long   uniqueID,
			  		long   rank,
			  		String location,
			  		String description,
			  		List<Long> connectionIDs) {
		    _nameFirst = nameFirst;
		    _nameLast  = nameLast;
		    _uniqueID  = uniqueID;
		    _location  = location;
		    _description = description;
		    _connectionIDs = connectionIDs;

	  }
	  /*
	   * http://leepoint.net/notes-java/oop/constructors/constructor.html
	   */
	  public PersonDummy() {
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
		 * PersonDummy data is not real, dummy!
		 */
	  @Override
	  public boolean isRealData() {
		return false;
	  }
  	 /*
  	  * getters & setters
  	  */
	  public void setNameFirst(String nameFirst) {
		   _nameFirst = nameFirst;
	  }
	  public String getNameFirst() {
		  return _nameFirst;
	  }
	  
	  public void setNameLast(String nameLast) {
		   _nameLast = nameLast;
	  }
	  public String getNameLast() {
		  return _nameLast;
	  }
	  
	  @Override
	  public void setNameFull(String nameFull) {
		 //do nothing
	  }
	  
	  @Override
	  public String getNameFull() {
		  return _nameFirst + " " + _nameLast;
	  }
	  
	  @Override
	  public void setUniqueID(long uniqueID) {
		  _uniqueID = uniqueID;
	  }
	  
	  @Override
	  public long getUniqueID() {
		  return _uniqueID;
	  }
	
	  
	  @Override
	  public List<Long> getConnectionIDs() {
		  return _connectionIDs;
	  }
	
	  @Override
	  public void setConnectionIDs(List<Long> connectionIDs) {
		  if (connectionIDs != _connectionIDs) {
			  List<Long> tmp = new ArrayList<Long>(); // Shouldn't need a temp if connectionIDs != this.connectionIDs
			  tmp.addAll(connectionIDs);
			  _connectionIDs = tmp;
		  }
	  }
	  
	  public void addConnectionIDs(List<Long> connectionIDs) {
		  if (_connectionIDs == null)
			  setConnectionIDs(connectionIDs);
		  else if (connectionIDs != null) {
			  _connectionIDs.addAll(connectionIDs);
		  }
	  }
	  
	  public static final int MAX_DESCRIPTION = 400;

	 
	  @Override
	  public void setDescription(String description) {
		  if (description!= null && description.length() > MAX_DESCRIPTION) {
			 logger.info("description (b)[" + description.length()+ "] " + description);
		  }
		  else {
			  _description = description;
		  }
	  }
	  
		@Override
		public String getEmployer() {
			return _employer;
		}
		
		@Override
		public void setEmployer(String employer) {
			_employer = employer;
		}
		
		@Override
		public void setLocation(String location) {
			_location = location;
		}
		
		@Override
		public String getLocation() {
	  		return _location;
	  	}
		
		@Override
		public String getHtmlPage() {
			return (_htmlPage != null ? _htmlPage.getValue() : null);
		}
		@Override
		public void setHtmlPage(String htmlPage) {
			if (htmlPage != null) {
				_htmlPage = new Text(htmlPage);
			}
		}
		
	 
	/*
	 * Server & DB management
	 *   
	 */
	
	
    @Override
	public String getDescription() {
  		return _description;
  	}
  	  	
  	public boolean isSameAs(PersonDummy person) {
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
	private static List<PersonDummy> getPersonsWithUniqueId(PersistenceManager pm, long uniqueID) {
  		logger.info("getPersonsWithUniqueId(" + uniqueID + ")");
  		List<PersonDummy> results = null;
  		Query query = pm.newQuery(PersonDummy.class);
  		query.setFilter("_uniqueID == uniqueIDParam");
  		query.setOrdering("_uniqueID asc");
  		query.declareParameters("long uniqueIDParam");
  		try {
  			results = (List<PersonDummy>) query.execute(uniqueID);
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
  	
	private static PersonDummy getPersonByUniqueId_(PersistenceManager pm, long uniqueID) {
  		PersonDummy person = null;
  		List<PersonDummy> results = getPersonsWithUniqueId(pm, uniqueID) ;
  		if (results != null) {
	  	    assert(results.size() <= 1);
	  	    if (results.size() >= 1){
	  	      	person = results.get(0);
	  	    } 
  		}
  		String nameFull = person != null ? person.getNameFull() : "none";
  		Statistics.getInstance().recordEvent("PersonDummy.getPersonByUniqueId_(" + uniqueID + ") = " + nameFull);
  		return person;
  	}
	
  	/*
  	 * Will a simple to = from work?
  	 */
	private static void copyFields(PersonDummy from, PersonDummy to) {
  		to._nameFirst = from._nameFirst;
  		to._nameLast  = from._nameLast;
  		to._uniqueID  = from._uniqueID;
  
  	    to._location = from._location;
	    to._description = from._description;
	    if (from._connectionIDs == null)
	    	to._connectionIDs = null;
	    else {
	    	List<Long> tmp = new ArrayList<Long>();
	    	tmp.addAll(from._connectionIDs);
		    to._connectionIDs = tmp;
	    }
	    to._htmlPage = from._htmlPage;

  	}
  	/*
  	 * http://code.google.com/appengine/docs/java/datastore/creatinggettinganddeletingdata.html
  	 */
  	public  void saveToDB() {
  		 		
  		PersonDummy p = null;
  		PersistenceManager pm = PMF.get().getPersistenceManager();;
  		try {	
  			p = getPersonByUniqueId_(pm, _uniqueID);
  			if (p != null) { 
  				// Person found in DB so update fields. 
  				// Person should get saved in close below
	  			assert(p.getUniqueID() == _uniqueID);
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
   		PersonDummy p = null;
  		PersistenceManager pm = PMF.get().getPersistenceManager();;
  		try {	
  			p = getPersonByUniqueId_(pm, _uniqueID);
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
	  			assert(p.getUniqueID() == _uniqueID);
	  			copyFields(this, p); 
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
						  		long   uniqueID,
						  		long   rank,
						  		String location,
						  		String description,
						  		List<Long> connectionIDs) {
  		PersonDummy entry = new PersonDummy( 
	    		nameFirst, 
		  		nameLast, 
		  		uniqueID,
		  		rank,
		  		location,
		  		description,
		  		connectionIDs );
  	
	    PersistenceManager pm = PMF.get().getPersistenceManager();
	    pm.makePersistent(entry);
	    pm.close();
  	}
  	
 	  	
	/*
	 * !Public interface to this module
	 * 
	 * Find person by their _uniqueID
	 * If no person found then return null
	 */
	public static PersonDummy findInDBbyUniqueId(long uniqueID) {
		Statistics.getInstance().recordEvent("PersonDummy.findInDBbyUniqueId(" + uniqueID + ")");
		PersistenceManager pm = PMF.get().getPersistenceManager();;
  		PersonDummy p = null;
  		try {
  			p = getPersonByUniqueId_(pm, uniqueID);
  			if (p != null) {
  				assert(p.getUniqueID() == uniqueID);
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
		return ReadState.RECORD_READ_FULL;
	}
	@Override
	public void setReadState(ReadState readState) {
	//	does nothing since readState is constatn
	}
	
	
	// !@#$ Hack for checking cache performance
	// This shows which cache the person was fetched from
	@NotPersistent
	String _whence = null;
	@Override
	public void setWhence(String whence) {
		logger.info("setWhence("+whence+") - " + (getNameFull()));
		_whence = whence;
	}
	@Override
	public String getWhence() {
		return _whence;
	}
	
	// !@#$ Hack for checking cache performance
	// This shows how long it took to fetch the person
	@NotPersistent
	double _fetchDuration = 0.0;
	@Override
	public void setFetchDuration(double fetchDuration) {
		_fetchDuration = fetchDuration;
	}
	@Override
	public double getFetchDuration() {
		return _fetchDuration;
	}
	
	@Override
	public boolean isIncomplete() {
		return false;
	}
	
	

}

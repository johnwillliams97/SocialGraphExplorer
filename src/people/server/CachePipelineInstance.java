package people.server;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import cache.CachePipeline;
import datatypes.PersonDummy;
import people.client.Misc;
import people.client.OurConfiguration;
import people.client.PersonClient;
import people.client.PersonTrait;
import people.client.Statistics;


class CachePipelineInstance {
	private static final Logger logger = Logger.getLogger(CachePipelineInstance.class.getName());
		
	private int _numFetches = 0;
	private int _numMemCacheFetches = 0;
	private int _numDBCacheFetches = 0;
	private int _numWTFCacheFetches = 0;
	private CachePipeline<Long, PersonDummy> _cachePipeline;
	private int _numCacheMisses = 0;
	private String _filler = null;
	
	public CachePipelineInstance(CachePipeline<Long, PersonDummy> cachePipeline) {
		_numFetches = 0;
		_cachePipeline = cachePipeline;
		_filler = "";
		String phrase = "Yada yada ";
		int len = phrase.length();
		int maxlen = OurConfiguration.HTML_DATA_MAX_SIZE;
		for (int i = 0; i < maxlen; i += len)
			_filler += phrase;
	}
	public PersonDummy get(long idIn, double timeBoundSec) {
		double start = Statistics.getCurrentTime();
		
		long id = mapUnknownID(idIn);
		PersonDummy person = _cachePipeline.get(id, timeBoundSec);
		cleanPersonConnections(person);
		++_numFetches;
		String whence = "none";
					
		if (person != null) {
			double end = Statistics.getCurrentTime();
			person.setFetchDuration(Misc.round3(end-start));
			whence = person.getWhence();
			if (whence != null) {
				if (whence.contains("CacheDB"))
					++_numDBCacheFetches;
				else if (whence.contains("CacheCache"))
					++_numMemCacheFetches;
				else {
					++_numWTFCacheFetches;
				}
			}
			if (person.getHtmlPage() == null && OurConfiguration.ADD_FAKE_HTML) {
				StringBuilder fragment = new StringBuilder();
				fragment.append("<b>" + makeGoogleLink(person.getNameFull()) + "</b>, ");
				fragment.append("<i>" + makeGoogleLink(person.getDescription()) + "</i>, ");
				fragment.append("<red><u>" + makeGoogleMapsLink(person.getLocation()) + "</u></red>, ");
				fragment.append("<blue>ID = <blink>" + makeInternalLink(person.getUniqueID()) + "</blink></blue>, ");
				fragment.append("<i><green>Fetched from the " + whence + " cache.<green></i>:   ");
				fragment.append("<grey>");
				for (Long uid: person.getConnectionIDs()) {
					fragment.append(makeInternalLink(uid) + ", ");
					if (fragment.length() > OurConfiguration.HTML_DATA_MAX_SIZE - 100)
						break;
				}
				fragment.append("</grey><br/>");
				String htmlFragment = fragment.toString();
				
				StringBuilder page = new StringBuilder();
				int numRepeats = OurConfiguration.HTML_DATA_MAX_SIZE/htmlFragment.length();
				for (int i = 0; i < numRepeats; ++i)
					page.append(htmlFragment);
				page.append(_filler);
				String htmlPage = page.toString();
				person.setHtmlPage(htmlPage);
				logger.warning("html size = " + person.getHtmlPage().length());
				
			}
		}
		else {
			++_numCacheMisses ;
		}
		return person;
	}
	
	private static String makeInternalLink(long id) {
		return makeLink("" + id, "/#key=");
	}
	private static String makeGoogleLink(String name) {
		return makeLink(name, "http://www.google.com/search?q=");
	}
	private static String makeGoogleMapsLink(String name) {
		return makeLink(name, "http://maps.google.com/maps?q=");
	}
	private static String makeLink(String name, String webSite) {
		String encName = "UNKNOWN";
		try {
			encName = URLEncoder.encode(name, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = webSite + encName;
		String link = "<a href=\"" + url + "\">" + name + "<a>";
		return link;
	}
	
	// All  PersistentPersonTrait classes have a DEFAULT_PERSON_RECORD_UNIQUEID
	// This is the value returned when the client requests a PersonTrait.GET_DEFAULT_PERSON_UNIQUEID
	// It is used for bootstrapping when the client doesn't know what is in the server database
	// 
	private long mapUnknownID(long id) {
		if (id == PersonTrait.GET_DEFAULT_PERSON_UNIQUEID)
			id = PersonDummy.DEFAULT_PERSON_RECORD_UNIQUEID;
		return id;
	}
	int getNumFetches() {
		return _numFetches;
	}
	int getNumMemCacheFetches() {
		return _numMemCacheFetches;
	}
	int getNumDBCacheFetches() {
		return _numDBCacheFetches;
	}
	
	static private void cleanPersonConnections(PersonDummy person) {
		if (person != null) {
			List<Long> connectionIDs = person.getConnectionIDs();
			if (connectionIDs != null) {
				Long id = person.getUniqueID();
				boolean removed = false;
				int  numRemoved = 0;
				while (true) {
					removed = connectionIDs.remove(id);
					if (!removed)
						break;
					++numRemoved;
				}
				if (numRemoved > 0) {// !@#$ fix data in database
					//logger.warning(person.getNameFull() + " [" + person.getUniqueID() + "] " 	+ numRemoved + " connections are unique ID");
				}
				connectionIDs = removeDuplicates(connectionIDs); // !@#$ fix data in database
				person.setConnectionIDs(connectionIDs);
			}
			PersonClient.debugValidate(person);
		}
	}
	
	private static List<Long> removeDuplicates(List<Long> list) {
	    Set<Long>  noDupsSet = new LinkedHashSet<Long>(list);
	    List<Long> noDupsList = new ArrayList<Long>(noDupsSet);
	    if (list.size() != noDupsSet.size())
	    	logger.warning("Removed " + (list.size() - noDupsSet.size()) + " duplicates");
	    return noDupsList;
	}
}

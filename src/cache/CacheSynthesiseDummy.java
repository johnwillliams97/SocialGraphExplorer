package cache;
/*
 * Wrapper for javax.cache.Cache
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.logging.Logger;

import people.client.Misc;
import people.client.OurConfiguration;
import datatypes.PersonDummy;


/*
 * This cache stage will create a PersonDummmy if none exists
 */

public class CacheSynthesiseDummy implements CacheActual<Long, PersonDummy> {
	private static final Logger logger = Logger.getLogger(CacheSynthesiseDummy.class.getName());

	
	private List<String> _nameFirst;
	private List<String> _nameLast;
	private List<String> _location;
	private List<String> _occupation;
	private List<String> _industry;
	
	public CacheSynthesiseDummy() {
		_nameFirst = readListFromFile("nameFirst.txt");
		_nameLast  = readListFromFile("nameLast.txt");
		_location  = readListFromFile("location.txt");
		_occupation  = readListFromFile("occupation.txt");
		_industry  = readListFromFile("industry.txt");
		
	}
	
	@Override
	public PersonDummy get(Long key,  double timeBoundSec) {
		PersonDummy person = synthesisePerson(key);
		if (person != null)
			person.setWhence("Synthesiser");
		return person;
	}

	
	@Override
	public void put(Long key, PersonDummy value) {
		// Not implemented
	}

	@Override
	public String identify() {
		return CacheCache.class.getSimpleName();
	}

	private static final long PRIME1 = 1125899839733759L;
	private static final long PRIME2 = 18014398241046527L;
	/*
	 * If a person does not exist then make one up!
	 */
	private PersonDummy synthesisePerson(long uniqueID) {
		PersonDummy person = new PersonDummy();
		String nameFirst = getFromList(_nameFirst, uniqueID);
		String nameLast1 = getFromList(_nameLast, uniqueID);
		String nameLast2 = getFromList(_nameLast, uniqueID*PRIME1 + PRIME2);
		String nameLast = (nameLast1 == nameLast2) ? nameLast1 : nameLast1 + "-" + nameLast2;
		String location = getFromList(_location, uniqueID);
		String occupation = getFromList(_occupation, uniqueID);
		String industry = getFromList(_industry, uniqueID);
		String description = occupation + " in the " + industry + " industry";
		List<Long> connectionIDs = makeConnectionIDs(uniqueID) ;
		person.setUniqueID(uniqueID);
		person.setNameFirst(nameFirst);
		person.setNameLast(nameLast);
		person.setLocation(location);
		person.setDescription(description);
		person.setConnectionIDs(connectionIDs);
		return person;
	}
	
	/*
	 * Make a topology of connected persons
	 * Connections are same in each direction
	 * Local groups and inter-group
	 * 
	 * (99 because a person is excluded from their own list
	 *	Local group = All persons within a 0..999 uniqueID range with same last digit => 98
	 *  Inter-group 1) = All persons with uniqueID -5..-1,+1..+5 => 10
	 *  Inter-group 2) = uniqueID + n*1000n = -5..-1,+1..+5 => 10
	 */
	private static final long LOCAL_RANGE = 1000L;
	private static final long LOCAL_MODULUS = 10L;
	private static final long INTER1_RANGE = 5L;
	private static final long INTER1_MODULUS = 1L;
	private static final long INTER2_RANGE   = 5000L;
	private static final long INTER2_MODULUS = 1000L;
	List<Long> makeConnectionIDs(long uniqueID) {
		List<Long> connectionIDs = new ArrayList<Long>();
		long base = (uniqueID/LOCAL_RANGE)*LOCAL_RANGE;
		long modulus = uniqueID % LOCAL_MODULUS;
		long i0 = base + modulus;
		for (long i = i0; i < base + LOCAL_RANGE; i += LOCAL_MODULUS) {
			if (i != uniqueID && inRange(i))
				connectionIDs.add(i);
		}
		for (long i = uniqueID - INTER1_MODULUS; i >= uniqueID - INTER1_RANGE; i -= INTER1_MODULUS) {
			if ((i-i0) % LOCAL_MODULUS != 0L && inRange(i))
				connectionIDs.add(i);
		}
		for (long i = uniqueID + INTER1_MODULUS; i <= uniqueID + INTER1_RANGE; i += INTER1_MODULUS) {
			if ((i-i0) % LOCAL_MODULUS != 0L && inRange(i))
				connectionIDs.add(i);
		}
		for (long i = uniqueID - INTER2_MODULUS; i >= uniqueID - INTER2_RANGE; i -= INTER2_MODULUS) {
			if (((i-i0) % LOCAL_MODULUS != 0L || i < base - INTER1_RANGE) && inRange(i))
				connectionIDs.add(i);
		}
		for (long i = uniqueID + INTER2_MODULUS; i <= uniqueID + INTER2_RANGE; i += INTER2_MODULUS) {
			if (((i-i0) % LOCAL_MODULUS != 0L || i > base + LOCAL_RANGE + - INTER1_RANGE) && inRange(i))
				connectionIDs.add(i);
		}
		Collections.sort(connectionIDs);
		int size = connectionIDs.size();
		for (int i = 1; i < size; ++i) {
			long l0 = connectionIDs.get(i-1);
			long l1 = connectionIDs.get(i);
			if (l0 == l1) {
				assert(l0 != l1);
			}
			if (l0 == uniqueID) {
				assert(l0 != uniqueID);
			}
			if (l1 == uniqueID) {
				assert(l1 != uniqueID);
			}
		}
		return connectionIDs;
		
	}
	
	/*
	 * This sets the distribution of a population of persons
	 */
	static boolean inRange(long i) {
		return (OurConfiguration.MINIMUM_UNIQUEID <= i && i <= OurConfiguration.MAXIMUM_UNIQUEID);
	}
	
	private static List<String> readListFromFile(String fileName) {
		List<String> strings = new ArrayList<String>();
 
		try {
			File file = new File(fileName);
			BufferedReader input = new BufferedReader(new FileReader(file));
		    String line = null  ; 
		    while (( line = input.readLine()) != null) {
		    	if (line.length() > 0)
		    		strings.add(line);
		    }
		    input.close();
		    logger.warning(fileName + " has " + strings.size() + " elements");
		} catch (FileNotFoundException e) {
			logger.warning("FileNotFoundException " + fileName + ", " + e.getMessage());
			Misc.reportException(e);
		} catch (IOException e) {
			logger.warning("IOException " + fileName + ", " + e.getMessage());
			Misc.reportException(e);
		}
		return strings;
	  }
	
	private String getFromList(List<String> list, long uniqueID) {
		int index = (int)(uniqueID % (long)list.size());
		return list.get(index);
	}

}


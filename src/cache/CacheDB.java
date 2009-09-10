package cache;
/*
 * Wrapper for DB access
 */


import datatypes.PersonLI;


public class CacheDB implements CacheActual<Long, PersonLI> {
	
	
	@Override
	public PersonLI get(Long key, WebReadPolicy policy, double timeBoundSec) {
		PersonLI 	person = PersonLI.findInDBbyUniqueId(key);
		return person;
	}
	
	@Override
	public void put(Long key, PersonLI value) {
		assert(key == value.getUniqueID());
		value.saveToDB();
	}

	@Override
	public String identify() {
		return CacheDB.class.getSimpleName();
	}

	
	
	
}


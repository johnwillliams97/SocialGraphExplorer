package cache;
/*
 * Wrapper for DB access
 */


import datatypes.PersonDummy;


public class CacheDB implements CacheActual<Long, PersonDummy> {
	
	
	@Override
	public PersonDummy get(Long key, double timeBoundSec) {
		PersonDummy person = PersonDummy.findInDBbyUniqueId(key);
		if (person != null)
			person.setWhence("CacheDB");
		return person;
	}
	
	@Override
	public void put(Long key, PersonDummy value) {
		assert(key == value.getUniqueID());
		value.saveToDB();
	}

	@Override
	public String identify() {
		return CacheDB.class.getSimpleName();
	}

	
	
	
}


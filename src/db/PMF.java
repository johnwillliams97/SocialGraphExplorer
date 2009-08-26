package db;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

public final class PMF {
	private static final PersistenceManagerFactory pmfInstance =
		JDOHelper.getPersistenceManagerFactory("transactions-optional");

	private PMF() {}
	
	private static int 		_referenceCount = 0;
	private static boolean 	_debug = false;

	public static PersistenceManagerFactory get() {
		++_referenceCount;
		if (_debug && _referenceCount != 1)
			return null;
		return pmfInstance;
	}
	
	/*
	  public static void close(PersistenceManager pm) {
	 	if (_debug && _referenceCount != 1)
			pm = null;
		pm.close();
		--_referenceCount;
	}
	*/
	
	private static PersistenceManager _queryPm = null;
	public static PersistenceManager getQueryPm() {
		if (_queryPm == null) {
			_queryPm = get().getPersistenceManager();
		}
		return _queryPm;
	}
	
}


package people.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/*
 * Miscellaneous collection functions
 */
public class MiscCollections {

	public static<T> List<T> arrayToList(T[] array) {
		List<T> list = null;
		if (array != null) {
			list = new ArrayList<T>();
			for (int i = 0; i < array.length; ++i) {
				list.add(array[i]);
			}
		}
		return list;
	}
	
	public static List<Long> arrayToListLong(long[] array) {
		List<Long> list = null;
		if (array != null) {
			list = new ArrayList<Long>();
			for (int i = 0; i < array.length; ++i) {
				list.add(array[i]);
			}
		}
		return list;
	}
	
	public static long[] listToArrayLong(Collection<Long> list) {
		long[] array = null;
		if (list != null) {
			array = new long[list.size()];
			//for (int i = 0; i < list.size(); ++i) {
			//	array[i] = list.get(i);
			//}
			int i = 0;
			for (Long e: list) {
				array[i++] = e;
			}
		}
		return array;
	}

	public static List<Integer> arrayToListInt(int[] array) {
		List<Integer> list = null;
		if (array != null) {
			list = new ArrayList<Integer>();
			for (int i = 0; i < array.length; ++i) {
				list.add(array[i]);
			}
		}
		return list;
	}
	
	public static  int[] listToArrayInt(List<Integer> list) {
		int[] array = null;
		if (list != null) {
			array = new int[list.size()];
			for (int i = 0; i < list.size(); ++i) {
				array[i] = list.get(i);
			}
		}
		return array;
	}
	static int sizeOfArray(long[][] array) {
		int size = 0;
		if (array != null) {
			for (int i = 0; i < array.length; ++i) {
				size += array[i].length;
			}
		}
		return size;
	}
	
	static long[] flattenArray(long[][] array) {
		long[] flattened = new long[sizeOfArray(array)];
		int  index = 0;
		if (array != null) {
			for (int i = 0; i < array.length; ++i) {
				for (int j = 0; j < array[i].length; ++j) {
					flattened[index++] += array[i][j];
				}
			}
		}
		return flattened;
	}
	
	public static String arrayToString(long[] ids) {
 		int numIDs = (ids != null) ? ids.length : 0;
 		String dbgIDs = "" + numIDs + ": [";
		for (int i = 0; i < numIDs; ++i)
	     	dbgIDs += "" + ids[i] + ", ";
	  	dbgIDs += "]";
	  	return dbgIDs;
	}

	/*
	 * Returns true if containerIDs contains anID
	 */
	public static boolean arrayContains(long[] containerIDs, long anID) {
		boolean contains = false;
		if (containerIDs != null) {
			for (Long id: containerIDs) {
				if (id == anID) {
					contains = true;
					break;
				}
			}
		}
		return contains;
	}
	/*
	 * Returns true if containerIDs contains all entries in containeeIDs
	 */
	public static boolean arrayContainsArray(long[] containerIDs, long[] containeeIDs) {
		boolean contains = true;
		if (containeeIDs != null) {
			for (Long id: containeeIDs) {
				if (!arrayContains(containerIDs, id)) {
					contains = false;
					break;
				}
			}
		}
		return contains;
	}
	
	/*
	 * Return the sublist of list from index i0 to i1
	 * Collection.sublist() is not implemented on GWT client
	 */
	static<T> List<T> getSubList(List<T> list, int i0, int i1) {
		List<T> subList = new ArrayList<T>();
		for (int i = i0; i <= i1; ++i) {
			subList.add(list.get(i));
		}
		return subList;
	}
	
	/*
	 * Remove elements in remover from removee
	 */
	static<T> void removeAll(Collection<T> removee, Collection<T> remover) {
		int  numRemoved = 0; // !@#$ for debugging 
		for (T e: remover) {
			boolean removed = false;
			while (true) {
				removed = removee.remove(e);
				if (!removed)
					break;
				++numRemoved;
			}
		}
	}
	
}
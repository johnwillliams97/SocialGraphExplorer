package people.client;

import java.util.ArrayList;
//import java.util.Calendar;
import java.util.List;
//import java.util.logging.Logger;



/*
 * Profiling
 */
public class Statistics {
//	private static final Logger logger = Logger.getLogger(Statistics.class.getName());
	
	private static Statistics _instance = null;
	private static int        _instanceCount = 0;
	
	
	public static Statistics getInstance() {
	    assert(_instance != null);
	    return _instance;
	}
	public static Statistics createInstance(String name) {
		++_instanceCount;
		_instance = new Statistics();
		_instance.recordEvent("Creation: <" + name + ", " +_instanceCount + ">");
	    return _instance;
	}
	
	public class Event {
		private double _when; // seconds since construction
		public String _name;
		public int    _sequence;
		public Event(String name) {
			this._name = name;
			this._sequence = ++_sequenceNumber;
			this._when = getCurrentTime();
		}
		public String describe() {
			String description = "Event[" + this._sequence + "] " + this._when + " sec: '" + this._name + "'";
			return description;
		}
	}
	
//	private long _callStartTime = 0L;
	private int  _sequenceNumber = 0;
	
	//Need to synchronise access to _events since this list is shared across servlets
	private List<Event> _events = null;
	
	private Statistics() {
		//this._callStartTime = Calendar.getInstance().getTimeInMillis();
		//this._callStartTime = System.nanoTime();
		this._sequenceNumber = 0;
		this._events = new ArrayList<Event>();
	}
	static public double getCurrentTime() {
	//	return (double)(Calendar.getInstance().getTimeInMillis() - _callStartTime)/1000.0;
	//	return (double)(System.nanoTime() - _callStartTime)/1.0e9;
		//return ((double)System.nanoTime())/1.0e9; // !@#$ Would like this for server
		return ((double)System.currentTimeMillis())/1.0e3;
	}
	
	public double getLastTime() {
		double lastTime = 0.0;
		synchronized(this) {
			int numEvents = this._events.size();
			if (numEvents > 0)
				lastTime = this._events.get(numEvents -1)._when;
		}
		return lastTime;
	}
	
	
	public  void recordEvent(String name) {
		Event event = new Event(name);
		synchronized(this) {  // _events exists across getPeople() calls
			this._events.add(event);
		}
		//logger.warning(event.describe());
	}
	
	public void showAllEvents() {
		System.err.println("=============== All events ============");
		synchronized(this) {
			for (Event event: this._events) {
				System.err.println(event.describe());
			}
		}
		System.err.println("=============== ---------- ============");
	}
	
	static private double roundBy(double x, double multiplier) {
		return ((double)Math.round(x*multiplier))/multiplier;
	}
	static public double round1(double x) {
		return roundBy(x, 10.0);
	}
	static public double round3(double x) {
		return roundBy(x, 1000.0);
	}
	
}

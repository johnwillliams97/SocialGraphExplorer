package people.client;

import java.util.ArrayList;
//import java.util.Calendar;
import java.util.List;



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
		private double _when; // seconds 
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
	
	private double _startTime = 0.0;
	private int    _sequenceNumber = 0;
	
	//Need to synchronise access to _events since this list is shared across servlets
	private List<Event> _events = null;
	
	private Statistics() {
		_sequenceNumber = 0;
		_events = new ArrayList<Event>();
		_startTime = getCurrentTime();
	}
	
	public static double getCurrentTime() {
		//return ((double)System.nanoTime())/1.0e9; // !@#$ Would like this for server
		return ((double)System.currentTimeMillis())/1.0e3;
	}
	
	public double getTimeSinceStart() {
		return getCurrentTime() - _startTime;
	}
	
	/*
	public double getLastTime() {
		double lastTime = 0.0;
		synchronized(this) {
			int numEvents = this._events.size();
			if (numEvents > 0)
				lastTime = this._events.get(numEvents -1)._when;
		}
		return lastTime;
	}
	*/
	
	public  void recordEvent(String name) {
		Event event = new Event(name);
		synchronized(this) {  // _events exists across getPeople() calls
			this._events.add(event);
		}
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
	
	
	
}

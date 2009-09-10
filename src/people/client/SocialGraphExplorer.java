package people.client;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 * 
 * 	http://examples.roughian.com/index.htm#Tutorials~History_Support
 * 	http://developer.yahoo.com/ypatterns/
 *  http://www.java2s.com/Code/Java/GWT
 */

import com.google.gwt.core.client.EntryPoint;
//import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;


public class SocialGraphExplorer 
			implements EntryPoint, ResizeHandler  {
	
	private ValueChangeHandler<String> historyHandler;
	
	private static SocialGraphExplorer singleton;
	/**
	   * Gets the singleton  instance.
	   */
	public static SocialGraphExplorer get() {
	    return singleton;
	}

  /**
   * Instantiate an application-level image bundle. This object will provide
   * programmatic access to all the images needed by widgets.
   */
 // private static final Images images = GWT.create(Images.class);

  /**
   * An aggregate image bundle that pulls together all the images for this
   * application into a single bundle.
   */
 // public interface Images extends Shortcuts.Images, TopPanel.Images {  }

  

 // x private TopPanel topPanel = new TopPanel(images);
  private VerticalPanel rightPanel = new VerticalPanel();
  private PersonList personList;
  private PersonDetail personDetail = new PersonDetail();
  private ServerStatus serverStatus = new ServerStatus();
 // private Shortcuts shortcuts = new Shortcuts(images);

  /**
   * Displays the specified person.
   * 
   * @param person
   */
  	public void displayPersonDetail(PersonClient person) {
  		personDetail.setPerson(person);
  	}
  
  	public void showStatus(String hdr, String msg) {
  		serverStatus.showStatus(hdr, msg, false);
  	}
  	
  	public void showStatus(String hdr, String msg, boolean bold) {
  		serverStatus.showStatus(hdr, msg, bold);
  	}
  	
  	public void statusFlush() {
  		serverStatus.flush();
  	}
  	
  	public void log(String header, String msg) {
  		log(header, msg, false);
  	}
  	
  	public void log(String header, String msg, boolean bold) {
  		statusFlush();
  		showStatus(header, msg, bold);
  		statusFlush();
  	}
  	
  	public void showError(String msg) {
  		log("Error",  msg);
  	}
  	public void showInstantStatus(String msg) {
  		serverStatus.showInstantStatus(msg, false);
 	}
  	public void showInstantStatus(String msg, boolean bold) {
  		serverStatus.showInstantStatus(msg, bold);
 	}
  	public void showInstantStatus2(String header, String body) {
  		serverStatus.showInstantStatus2(header, body, false);
 	}
  	public void showInstantStatus2(String header, String body, boolean bold) {
  		serverStatus.showInstantStatus2(header, body, bold);
 	}
    	

  /**
   * This method constructs the application user interface by instantiating
   * controls and hooking up event handler.
   */
  	public void onModuleLoad() {
  		singleton = this;

  	//Web history handling
		historyHandler = new ValueChangeHandler<String>() {
			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				String stringRep = event.getValue();
				SystemState systemState = new SystemState(stringRep);
				CanonicalState canonicalState = new CanonicalState(systemState.getUniqueID(), systemState.getIndex());
				personList.updatePersonListExtern(canonicalState, true);
			}
		};
		History.addValueChangeHandler(historyHandler);
		
		// Determine the system's starting state. null => default
		final String INITIAL_UI_STATE = OurConfiguration.INITIAL_UI_STATE; 
		String initToken = History.getToken();
		String stateString = (initToken != null && initToken.length() > 0) ? initToken : INITIAL_UI_STATE;
		
		SystemState systemState = new SystemState(stateString);
		CanonicalState canonicalState = new CanonicalState(systemState.getUniqueID(), systemState.getIndex());
		
		// PersonList uses LoadsOfFun.get() in its constructor, so initialise it after
		// 'singleton'.
		personList = new PersonList(canonicalState,
				systemState.getMaxServerCallsPerRequest(),
				systemState.getMaxRequestsInProgress(),
				systemState.getMaxServerCallsInProgress());
		
		personList.setWidth("100%");

		// Create the right panel, containing the email list & details.
		rightPanel.add(personList);
		rightPanel.add(serverStatus);
		rightPanel.add(personDetail);
    
		personList.setWidth("100%");
		personDetail.setWidth("100%");
		serverStatus.setWidth("100%");

       // Create a dock panel that will contain the menu bar at the top,
       // the shortcuts to the left, and the mail list & details taking the rest.
       DockPanel outer = new DockPanel();
       //    outer.add(topPanel, DockPanel.NORTH);
       // outer.add(shortcuts, DockPanel.WEST);
       outer.add(rightPanel, DockPanel.CENTER);
       outer.setWidth("100%");

       outer.setSpacing(4);
       outer.setCellWidth(rightPanel, "100%");

       // Hook the window resize event, so that we can adjust the UI.
       Window.addResizeHandler(this);

       // Get rid of scrollbars, and clear out the window's built-in margin,
       // because we want to take advantage of the entire client area.
       Window.enableScrolling(false);
       Window.setMargin("0px");

       // Finally, add the outer panel to the RootPanel, so that it will be
       // displayed.
       RootPanel.get().add(outer);

       // Call the window resized handler to get the initial sizes setup. Doing
       // this in a deferred command causes it to occur after all widgets' sizes
       // have been computed by the browser.
       DeferredCommand.addCommand(new Command() {
    	   public void execute() {
    		   onWindowResized(Window.getClientWidth(), Window.getClientHeight());
    	   }
       	});

       onWindowResized(Window.getClientWidth(), Window.getClientHeight());
       
  	}

  	public void onResize(ResizeEvent event) {
  		onWindowResized(event.getWidth(), event.getHeight());
  	}

  	public void onWindowResized(int width, int height) {
    // Adjust the shortcut panel and detail area to take up the available room
    // in the window.
	  /*
    int shortcutHeight = height - shortcuts.getAbsoluteTop() - 8;
    if (shortcutHeight < 1) {
      shortcutHeight = 1;
    }
    shortcuts.setHeight(shortcutHeight + "px");
    */
	//int listHeight =  personList.getAbsoluteTop();

    // Give the person detail widget a chance to resize itself as well.
	  
	  if (OurConfiguration.DEBUG_MODE) {
		  personDetail.adjustSize(width, height/10);
		  serverStatus.adjustSize(width, height);
	  }
	  else {
		  personDetail.adjustSize(width, height);
		  serverStatus.adjustSize(width, 0);
	  }
  }



}

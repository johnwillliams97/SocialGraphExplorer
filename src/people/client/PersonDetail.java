
package people.client;

import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * A composite for displaying the details of an email message.
 */
public class PersonDetail extends Composite {

  private VerticalPanel panel = new VerticalPanel();
  private HTML body = new HTML();
  private ScrollPanel scroller = new ScrollPanel(body);

  public PersonDetail() {
    body.setWordWrap(true);
    DockPanel innerPanel = new DockPanel();
    innerPanel.add(scroller, DockPanel.CENTER);
    innerPanel.setCellHeight(scroller, "100%");
    panel.add(innerPanel);
    innerPanel.setSize("100%", "100%");
    scroller.setSize("100%", "100%");
    initWidget(panel);

    setStyleName("mail-Detail");
    innerPanel.setStyleName("mail-DetailInner");
    body.setStyleName("mail-DetailBody");
  }

  /**
   * Adjusts the widget's size such that it fits within the window's client
   * area.
   */
  public void adjustSize(int windowWidth, int windowHeight) {
    int scrollWidth = windowWidth - scroller.getAbsoluteLeft() - 9;
    if (scrollWidth < 1) {
      scrollWidth = 1;
    }

    int scrollHeight = windowHeight - scroller.getAbsoluteTop() - 9;
    if (scrollHeight < 1) {
      scrollHeight = 1;
    }

    scroller.setPixelSize(scrollWidth, scrollHeight);
  }

  private static String makeRow(String key, String value) {
	  String row = "<li><b>" + key + "</b>:" ;
	  if (value != null) {
		  row += " " + value;
	  }
	  row += "</li>";
	  return row;
  }
  public void setPerson(PersonClient person) {
	  String html = "";
	  if (person != null) {
		  String personData = person.getHtmlPage();
		  if (personData != null) {
			  html += personData;
		  }
		  else {
			//  html += makeLiIframe(person);	
			  html += "<h1>" + person.getNameFull() + "</h1>";
		  }
	  }
	  body.setHTML(html);
  }
  public void setPersonOld(PersonClient person) {
	  	  String html = "";
	 	  if (person != null) {
	 		  //html += makeLiIframe(person);
	 		  
	 		  String connections = "";
	 		  List<Long> connectionIDs = person.getConnectionIDs();
	 		  int numConnections = Math.min(10, connectionIDs != null ? connectionIDs.size() : 0);
	 		  connections = "" + numConnections + " [";
	 		  for (int i = 0; i < numConnections; ++i) {
	 			  connections += " " + connectionIDs.get(i) + ",";
	 		  }
	 		  connections += "]";
	 		
	 		  html += makeRow("Name",        person.getNameFull());
	 		  html += makeRow("Description", person.getDescription());
	 		  html += makeRow("Location",    person.getLocation());
	 		  html += makeRow("Unique ID",   "" + person.getLiUniqueID());
	 		  html += makeRow("Connections", connections);

	 		//  html += "<p>" + person.getEmployer() + "</p>";
	 	  }
	 	  body.setHTML(html);
	   }
  
 
  /*
   * Linked URL builders
   */
  static private String _linkedInBase = "http://www.linkedin.com/";
	static String makeLiProfileName(long key) {
		String name = _linkedInBase + "profile?viewProfile=&key=" + key;
		return name;		
	}
	static String makeLiConnectionsName(long key) {
		String name = _linkedInBase + "profile?viewConns=&key=" + key;
		return name;		
	}
   	
   
}

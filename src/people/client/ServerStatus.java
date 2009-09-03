
package people.client;

import java.util.ArrayList;
import java.util.List;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Displays debug info on client UI
 */
public class ServerStatus extends Composite {

  private VerticalPanel panel = new VerticalPanel();
  private HTML body = new HTML();
  private ScrollPanel scroller = new ScrollPanel(body);

  public ServerStatus() {
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

   
  private final int  entriesPerLine = 4;
  private final int  numBufferLines = 200;
  private final int  numInstantStatuses = 40;
  
 
  private String instantStatus = new String();
  private int instantStatusCount = 0;
  private  List<String> instantStatusHistory = new ArrayList<String>();
  
  private String line = new String();
  private List<String> history = new ArrayList<String>();
  private int unflushedEntries = 0;
  private int totalLineCount = 0;


    
  public void flush() {
	  if (this.unflushedEntries > 0) {
		  this.unflushedEntries = 0;
		  this.history.add(this.line);
		  // Echo to console
		 // System.err.println("$>" + this.line);
		  this.line = "";
		  if (this.history.size() > this.numBufferLines) {
			 this.history.remove(0);
		  }
		  ++totalLineCount;
		  redraw();
	  }
  }
  
  private void redraw() {
	  String html = "";
	  html += "<b>" + this.instantStatusCount + ":</b><i>" + this.instantStatus + "</i>, ";
	  for (int i = this.instantStatusHistory.size() - 2; i >= 0; --i ) {
		  int n = this.instantStatusCount - 1 - (this.instantStatusHistory.size() - 2 -i);
		  html += "<b>" + n + ":</b><i>" + this.instantStatusHistory.get(i) + "</i>, ";
	  }
	 
	  html += "<table>\n";
	  for (int i = this.history.size() - 1; i >= 0; --i ) {
		  html += " <tr><td>";
		  String item = this.history.get(i);
		  html += item ;
		  html += " </td></tr>\n";
	  }
	  html += "</table>\n";
	 
	  body.setHTML(html);
 }
  
  public void showInstantStatus(String msg, boolean bold) {
	  if (bold) 
		  msg = "<b>" + msg + "</b>";
	  this.instantStatus = msg;
	  ++this.instantStatusCount;
	  this.instantStatusHistory.add(this.instantStatus);
	  this.line = "";
	  if (this.instantStatusHistory.size() > this.numInstantStatuses) {
		 this.instantStatusHistory.remove(0);
	  }
	  redraw();
	  System.err.println("IS>" + msg);
  }
  
  public void showInstantStatus2(String header, String body, boolean bold) {
	  String msg = "";
	  if (header != null)
		  msg += header;
	  msg += "(";
	  if (body != null)
		  msg += body;
	  msg += ")";
	  showInstantStatus(msg, bold);
  }
  
  public void showStatus(String header, String msg, boolean bold) {
	  if (header == null)
		  header = "?";
	  if (msg == null)
		  msg = "??";
	  System.err.println(header + ": " + msg);
	  String item = "" + this.instantStatusCount + ": <b>" + header + " = </b>" + msg + "<b>;</b> ";
	  if (bold)
		  item = "<blue><u>" + item + "</u></blue>";
	  this.line += item;
	  ++this.unflushedEntries;
	  if (this.unflushedEntries >= this.entriesPerLine) {
		  flush();
	  }
   }

   	
   
}

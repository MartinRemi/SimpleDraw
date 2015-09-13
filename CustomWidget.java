
public class CustomWidget {

	// These are status codes returned to a client.
	public static final int S_EVENT_NOT_CONSUMED = 0; // event not processed
	public static final int S_DONT_REDRAW = 1; // event was processed, and no need to redraw
	public static final int S_REDRAW = 2; // event was processed, and please redraw

	protected boolean isVisible = false;

	public boolean isVisible() { return isVisible; }
	public void setVisible( boolean flag ) { isVisible = flag; }

	public boolean isMouseOverWidget() { return false; }

	// Each of these returns a status code.
	public int pressEvent( int x, int y ) { return S_DONT_REDRAW; }
	public int releaseEvent( int x, int y ) { return S_DONT_REDRAW; }
	public int moveEvent( int x, int y ) { return S_DONT_REDRAW; }
	public int dragEvent( int x, int y ) { return S_DONT_REDRAW; }
	public void draw( GraphicsWrapper gw ) { }
}

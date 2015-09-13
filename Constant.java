
public class Constant {
	public static final int INITIAL_WINDOW_WIDTH = 512;
	public static final int INITIAL_WINDOW_HEIGHT = 512;

	public static final boolean BIG_TEXT_LABELS = false;

	// These are in pixels.
	public static final int TEXT_HEIGHT = BIG_TEXT_LABELS ? 14 : 10;
	public static final int MARGIN_AROUND_TEXT = BIG_TEXT_LABELS ? 6 : 4;
	public static final int MARGIN_BETWEEN_ITEMS = BIG_TEXT_LABELS ? 6 : 5;

	public static final float zoomFactorPerPixelDragged = 1.005f;

	public static final float MENU_ALPHA = 0.6f;
}


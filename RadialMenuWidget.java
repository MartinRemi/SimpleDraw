
import java.awt.Color;

public class RadialMenuWidget extends CustomWidget {

	// The radial menu has a central menu item (with index 0)
	// and up to 8 surrounding menu items
	// (with indices 1 through 8, numbered clockwise,
	// with 1 for North, 2 for North-East, ..., 8 for North-West).

	public static final int CENTRAL_ITEM = 0;
	public static final int NORTH      = 1;
	public static final int NORTH_EAST = 2;
	public static final int EAST       = 3;
	public static final int SOUTH_EAST = 4;
	public static final int SOUTH      = 5;
	public static final int SOUTH_WEST = 6;
	public static final int WEST       = 7;
	public static final int NORTH_WEST = 8;
	private static final int N = 8;

	// Each menu item has a corresponding ``label'' string.
	// If a given label string is empty (""),
	// then there is no menu item displayed for it.
	// In addition, the client can temporarily deactivate
	// an existing menu item by setting its ``isEnabled''
	// flag to false.
	private String [] label = new String[ N + 1 ];
	private boolean [] isEnabled = new boolean[ N + 1 ];

	// Each menu item also has a (normally distinct) ID number.
	// These are useful for causing multiple items to hilite together:
	// whenever the user drags over a given item,
	// it and all other items with the same ID hilite together.
	// This is intended for cases where there are redundant menu items
	// that map to the same function in the client's code.
	private int [] itemID = new int[ N + 1 ];

	private int selectedItem; // in the range [CENTRAL_ITEM,N]

	// pixel coordinates of center of menu
	protected int x0 = 0, y0 = 0;

	// pixel coordinates of current mouse position
	private int mouse_x, mouse_y;


	// These are in pixels.
	public static final int radiusOfNeutralZone = 10;
	public static final int textHeight = Constant.TEXT_HEIGHT;
	public static final int marginAroundText = Constant.MARGIN_AROUND_TEXT;
	public static final int marginBetweenItems = Constant.MARGIN_BETWEEN_ITEMS;

	public static final Color foregroundColor = new Color( 0, 0, 0 );
	public static final Color foregroundColor2 = new Color( 127, 127, 127 );
	public static final Color backgroundColor = new Color( 255, 255, 255 );

	public RadialMenuWidget() {
		for (int i = 0; i <= N; ++i) {
			label[i] = new String("");
			isEnabled[i] = true;

			// Give every item a distinct ID.
			itemID[i] = i;
		}
	}

	public void setItemLabelAndID( int index, String s, int id ) {
		if ( 0 <= index && index <= N ) {
			label[index] = s;
			itemID[index] = id;
		}
	}
	public void setItemLabel( int index, String s ) {
		if ( 0 <= index && index <= N ) {
			label[index] = s;
		}
	}
	public int getItemID( int index ) {
		if ( 0 <= index && index <= N ) {
			return itemID[index];
		}
		return -1;
	}

	public void setEnabledByID( boolean flag, int id ) {
		for (int i = 0; i <= N; ++i) {
			if ( itemID[i] == id ) {
				isEnabled[i] = flag;
			}
		}
	}

	// For internal use only.
	private boolean isItemHilited( int index ) {
		assert 0 <= index && index <= N;
		return itemID[ index ] == itemID[ selectedItem ];
	}

	// The client typically calls this after an interaction with the menu
	// is complete, to find out what the user selected.
	// Returns an index in the range [CENTRAL_ITEM,N]
	public int getSelection() { return selectedItem; }

	public int getIDOfSelection() { return getItemID( selectedItem ); }

	// Returns a status code.
	public int pressEvent( int x, int y ) {
		x0 = mouse_x = x;
		y0 = mouse_y = y;
		selectedItem = CENTRAL_ITEM;
		isVisible = true;
		return S_REDRAW;
	}
	public int releaseEvent( int x, int y ) {
		if ( isVisible ) {
			isVisible = false;
			return S_REDRAW;
		}
		return S_EVENT_NOT_CONSUMED;
	}
	public int moveEvent( int x, int y ) {
		// make the center of the menu follow the cursor
		x0 = mouse_x = x;
		y0 = mouse_y = y;
		return S_REDRAW;
	}
	public int dragEvent( int x, int y ) {
		if ( ! isVisible )
			return S_EVENT_NOT_CONSUMED;

		mouse_x = x;
		mouse_y = y;
		int dx = mouse_x - x0;
		int dy = mouse_y - y0;
		float radius = (float)Math.sqrt( dx*dx + dy*dy );

		int newlySelectedItem = CENTRAL_ITEM;

		if ( radius > radiusOfNeutralZone ) {
			float theta = (float)Math.asin( dy / radius );
			if ( dx < 0 ) theta = (float)Math.PI - theta;

			// theta is now relative to the +x axis, which points right,
			// and increases clockwise (because y+ points down).
			// If we added pi/2 radians, it would be relative to the -y
			// axis (which points up).
			// However, what we really want is for it to be relative to
			// the radial line that divides item 1 from item 8.
			// So we must add pi/2 + pi/8 radians.

			theta += 5 * (float)Math.PI / 8;

			// Ensure it's in [0,2*pi]
			assert theta > 0;
			if ( theta > 2*Math.PI ) theta -= 2*(float)Math.PI;

			newlySelectedItem = 1 + (int)( theta / ((float)Math.PI / 4) );
			assert 1 <= newlySelectedItem && newlySelectedItem <= N;

			if ( label[ newlySelectedItem ].length() == 0 || ! isEnabled[ newlySelectedItem ] ) {
				// loop over all items, looking for the closest one
				float minDifference = 4*(float)Math.PI;
				int itemWithMinDifference = CENTRAL_ITEM;
				for ( int candidateItem = 1; candidateItem <= N; ++candidateItem ) {
					if ( label[ candidateItem ].length() > 0 && isEnabled[ candidateItem ] ) {
						float candidateItemTheta = (candidateItem-1) * ((float)Math.PI/4) + (float)Math.PI/8;
						float candidateDifference = Math.abs( candidateItemTheta - theta );
						if ( candidateDifference > Math.PI )
							candidateDifference = 2*(float)Math.PI - candidateDifference;
						if ( candidateDifference < minDifference ) {
							minDifference = candidateDifference;
							itemWithMinDifference = candidateItem;
						}
					}
				}
				newlySelectedItem = itemWithMinDifference;
			}
		}

		if ( newlySelectedItem != selectedItem ) {
			selectedItem = newlySelectedItem;
			return S_REDRAW;
		}

		return S_DONT_REDRAW;
	}

	protected void drawMenuItems(
		GraphicsWrapper gw,
		boolean drawOnlyHilitedItem, // if false, all menu items are drawn
		boolean drawUsingPieStyle,
		int radiusOfPie // only used if ``drawUsingPieStyle'' is true
	) {
		final float alpha = Constant.MENU_ALPHA;

		if ( drawUsingPieStyle ) {
			gw.setColor( foregroundColor2, alpha );
			gw.fillCircle(
				x0-radiusOfPie, y0-radiusOfPie,
				radiusOfPie
			);
		}

		if ( isItemHilited( CENTRAL_ITEM ) )
			gw.setColor( foregroundColor, alpha );
		else
			gw.setColor( backgroundColor, alpha );
		gw.fillCircle(
			x0-radiusOfNeutralZone, y0-radiusOfNeutralZone,
			radiusOfNeutralZone
		);
		if ( ! isItemHilited( CENTRAL_ITEM ) )
			gw.setColor( foregroundColor );
		else
			gw.setColor( backgroundColor );
		gw.drawCircle(
			x0-radiusOfNeutralZone, y0-radiusOfNeutralZone,
			radiusOfNeutralZone
		);


		/*
			Below we have the upper right quadrant of the radial menu.
				+---------+              \
				| item 1  |               ) heightOfItems
				+---------+              /
				     .                    ) marginBetweenItems
				     . +---------+       \
				     . | item 2  |        ) heightOfItems
				     . +---------+       /
				     . .                  ) marginBetweenItems
				     ..     +---------+  \
				     o......| item 3  |   ) heightOfItems
				            +---------+  /
			Let r be the distance from the menu's center "o" to the center of item 1,
			and also the distance from "o" to the center of item 3.
			From the picture, we have
				r == heightOfItems / 2 + marginBetweenItems + heightOfItems
				     + marginBetweenItems + heightOfItems / 2
				  == 2 * ( heightOfItems + marginBetweenItems )
			Let r' be the distance from "o" to the center of item 2.
			This distance is measured along a line that slopes at 45 degrees.
			Hence
				r'/sqrt(2) == heightOfItems / 2 + marginBetweenItems
				              + heightOfItems / 2
				r' == sqrt(2) * ( heightOfItems + marginBetweenItems )
				   == r / sqrt(2)
		*/
		int heightOfItem = textHeight + 2*marginAroundText;
		float radius = 2*( heightOfItem + marginBetweenItems );
		float radiusPrime = radius / (float)Math.sqrt(2.0f);

		for ( int i = 1; i <= N; ++i ) {
			if ( label[i].length() > 0 && isEnabled[i] ) {
				float theta = (float)( (i-1)*Math.PI/4 - Math.PI/2 );
				// compute center of ith label
				float x = ( (i%2)==1 ? radius : radiusPrime ) * (float)Math.cos( theta ) + x0;
				float y = ( (i%2)==1 ? radius : radiusPrime ) * (float)Math.sin( theta ) + y0;

				if ( i == 1 && label[2].length() == 0 && label[8].length() == 0 ) {
					y = -radius/2 + y0;
				}
				else if ( i == 5 && label[4].length() == 0 && label[6].length() == 0 ) {
					y = radius/2 + y0;
				}

				float stringWidth = gw.stringWidth( label[i] );
				float widthOfItem = stringWidth + 2*marginAroundText;

				// We want items that appear side-by-side to have the same width,
				// so that the menu is symmetrical about a vertical axis.
				if ( i!=1 && i!=5 && label[N+2-i].length() > 0 ) {
					float otherStringWidth = gw.stringWidth( label[N+2-i] );
					if ( otherStringWidth > stringWidth )
						widthOfItem = otherStringWidth + 2*marginAroundText;
				}

				if ( 2 == i || 4 == i ) {
					if ( x - widthOfItem/2 <= x0 + marginBetweenItems )
						// item is too far to the left; shift it to the right
						x = x0 + marginBetweenItems + widthOfItem/2;
				}
				else if ( 3 == i ) {
					if ( x - widthOfItem/2 <= x0 + radiusOfNeutralZone + marginBetweenItems )
						// item is too far to the left; shift it to the right
						x = x0 + radiusOfNeutralZone + marginBetweenItems + widthOfItem/2;
				}
				else if ( 6 == i || 8 == i ) {
					if ( x + widthOfItem/2 >= x0 - marginBetweenItems )
						// item is too far to the right; shift it to the left
						x = x0 - marginBetweenItems - widthOfItem/2;
				}
				else if ( 7 == i ) {
					if ( x + widthOfItem/2 >= x0 - radiusOfNeutralZone - marginBetweenItems )
						// item is too far to the right; shift it to the left
						x = x0 - radiusOfNeutralZone - marginBetweenItems - widthOfItem/2;
				}

				if ( isItemHilited( i ) )
					gw.setColor( foregroundColor, alpha );
				else
					gw.setColor( backgroundColor, alpha );
				gw.fillRect(
					x - widthOfItem/2, y - heightOfItem/2,
					widthOfItem, heightOfItem
				);
				if ( ! isItemHilited( i ) )
					gw.setColor( foregroundColor );
				else
					gw.setColor( backgroundColor );
				gw.drawRect(
					x - widthOfItem/2, y - heightOfItem/2,
					widthOfItem, heightOfItem
				);
				gw.drawString(
					Math.round( x - stringWidth/2 ),
					Math.round( y + textHeight/2 ),
					label[i]
				);
			}
		}
	}

	public void draw(
		GraphicsWrapper gw
	) {
		if ( ! isVisible )
			return;

		drawMenuItems( gw, false, false, 0 );
	}

}


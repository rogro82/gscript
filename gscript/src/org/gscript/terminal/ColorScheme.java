package org.gscript.terminal;

public class ColorScheme {
	
	public static final int INDEX_BLACK 	= 0;
	public static final int INDEX_RED 		= 1;
	public static final int INDEX_GREEN 	= 2;
	public static final int INDEX_YELLOW 	= 3;
	public static final int INDEX_BLUE 		= 4;
	public static final int INDEX_MAGENTA 	= 5;
	public static final int INDEX_CYAN 		= 6;
	public static final int INDEX_WHITE 	= 7;
	
	
	public static final int[] COLOR_VALUES_NORMAL =
	{
		0xff000000,		/* black */
		0xff800000,		/* red */
		0xff008000,		/* green */
		0xff808000,		/* yellow */
		0xff002080,		/* blue */
		0xff800080,		/* magenta */
		0xff008080,		/* cyan */
		0xffc0c0c0,		/* light grey */
	};
	public static final int[] COLOR_VALUES_BRIGHT =
	{
		0xff808080,		/* dark grey */
		0xffff0000,		/* red */
		0xff00ff00,		/* green */
		0xffffff00,		/* yellow */
		0xff0000ff,		/* blue */
		0xffff00ff,		/* magenta */
		0xff00ffff,		/* cyan */
		0xffffffff,		/* white */
	};
	
	public static int getColor(int index, boolean bright) {
		return bright ? COLOR_VALUES_BRIGHT[index < 0 ? 0 : index > 7 ? 7 : index] : COLOR_VALUES_NORMAL[index < 0 ? 0 : index > 7 ? 7 : index];
	}
	
	public static final CharSequence[] SEQUENCE_COLOR_INDICES = 
		{ "0","1","2","3","4","5","6","7" };

	public static final CharSequence[] SEQUENCE_COLOR_NAMES = 
		{ "Black", "Red", "Green", "Yellow", "Blue", "Magenta", "Cyan", "White" };	
	
}

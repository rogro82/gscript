package org.gscript.terminal;

public class TextEffects {
	
	public static final int NONE =		0;
	public static final int BOLD = 		0x1;
	public static final int ITALIC = 	0x2;
	public static final int UNDERLINE = 0x4;
	public static final int BLINK = 	0x8;
	public static final int INVERSE = 	0x10;
	public static final int INVISIBLE = 0x20;
	public static final int IGNORE = 	0x40;
	
	public static boolean hasTextEffect(byte val, int option) {
		return ((val & option) != 0);
	}
}

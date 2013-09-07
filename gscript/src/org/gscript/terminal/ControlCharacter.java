package org.gscript.terminal;

/**
 * Terminal Control Characters
 */
public class ControlCharacter {
	/** NULL */
	static final byte NUL = 0x0;
	/** ENQ */
	static final byte ENQ = 0x5;
	/** BELL */
	static final byte BEL = 0x7;
	/** BACKSPACE */
	static final byte BS = 0x8;
	/** HORIZONTAL TABULATION */
	static final byte HT = 0x9;
	/** LINE FEED */
	static final byte LF = 0xa;
	/** VERTICAL TABULATION */
	static final byte VT = 0xb;
	/** FORM FEED */
	static final byte FF = 0xc;
	/** CARRIAGE RETURN */
	static final byte CR = 0xd;
	/** SHIFT OUT */
	static final byte SO = 0xe;
	/** SHIFT IN */
	static final byte SI = 0xf;
	/** X-ON */
	static final byte XON = 0x11;
	/** X-OFF */
	static final byte XOF = 0x13;	
	/** CANCEL */
	static final byte CAN = 0x18;
	/** SUBSTITUTE */
	static final byte SUB = 0x1a;
	/** ESCAPE */
	static final byte ESC = 0x1b;
	/** DELETE */
	static final byte DEL = 0x7f;
	/** COMMAND SEQUENCE INITIATOR ( ESC [ ) */
	static final byte CSI = (byte) 0x9b;
}	
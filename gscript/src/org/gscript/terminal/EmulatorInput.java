package org.gscript.terminal;

import org.gscript.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

public class EmulatorInput extends LinearLayout implements OnClickListener,
		OnCheckedChangeListener {

	static final String LOG_TAG = "EmulatorInput";

	/* keycodes and metastates for backward compatibility */
	
	public static final int KEYCODE_UNKNOWN = 0;
	/**
	 * Key code constant: Soft Left key. Usually situated below the display on
	 * phones and used as a multi-function feature key for selecting a software
	 * defined function shown on the bottom left of the display.
	 */
	public static final int KEYCODE_SOFT_LEFT = 1;
	/**
	 * Key code constant: Soft Right key. Usually situated below the display on
	 * phones and used as a multi-function feature key for selecting a software
	 * defined function shown on the bottom right of the display.
	 */
	public static final int KEYCODE_SOFT_RIGHT = 2;
	/**
	 * Key code constant: Home key. This key is handled by the framework and is
	 * never delivered to applications.
	 */
	public static final int KEYCODE_HOME = 3;
	/** Key code constant: Back key. */
	public static final int KEYCODE_BACK = 4;
	/** Key code constant: Call key. */
	public static final int KEYCODE_CALL = 5;
	/** Key code constant: End Call key. */
	public static final int KEYCODE_ENDCALL = 6;
	/** Key code constant: '0' key. */
	public static final int KEYCODE_0 = 7;
	/** Key code constant: '1' key. */
	public static final int KEYCODE_1 = 8;
	/** Key code constant: '2' key. */
	public static final int KEYCODE_2 = 9;
	/** Key code constant: '3' key. */
	public static final int KEYCODE_3 = 10;
	/** Key code constant: '4' key. */
	public static final int KEYCODE_4 = 11;
	/** Key code constant: '5' key. */
	public static final int KEYCODE_5 = 12;
	/** Key code constant: '6' key. */
	public static final int KEYCODE_6 = 13;
	/** Key code constant: '7' key. */
	public static final int KEYCODE_7 = 14;
	/** Key code constant: '8' key. */
	public static final int KEYCODE_8 = 15;
	/** Key code constant: '9' key. */
	public static final int KEYCODE_9 = 16;
	/** Key code constant: '*' key. */
	public static final int KEYCODE_STAR = 17;
	/** Key code constant: '#' key. */
	public static final int KEYCODE_POUND = 18;
	/**
	 * Key code constant: Directional Pad Up key. May also be synthesized from
	 * trackball motions.
	 */
	public static final int KEYCODE_DPAD_UP = 19;
	/**
	 * Key code constant: Directional Pad Down key. May also be synthesized from
	 * trackball motions.
	 */
	public static final int KEYCODE_DPAD_DOWN = 20;
	/**
	 * Key code constant: Directional Pad Left key. May also be synthesized from
	 * trackball motions.
	 */
	public static final int KEYCODE_DPAD_LEFT = 21;
	/**
	 * Key code constant: Directional Pad Right key. May also be synthesized
	 * from trackball motions.
	 */
	public static final int KEYCODE_DPAD_RIGHT = 22;
	/**
	 * Key code constant: Directional Pad Center key. May also be synthesized
	 * from trackball motions.
	 */
	public static final int KEYCODE_DPAD_CENTER = 23;
	/**
	 * Key code constant: Volume Up key. Adjusts the speaker volume up.
	 */
	public static final int KEYCODE_VOLUME_UP = 24;
	/**
	 * Key code constant: Volume Down key. Adjusts the speaker volume down.
	 */
	public static final int KEYCODE_VOLUME_DOWN = 25;
	/** Key code constant: Power key. */
	public static final int KEYCODE_POWER = 26;
	/**
	 * Key code constant: Camera key. Used to launch a camera application or
	 * take pictures.
	 */
	public static final int KEYCODE_CAMERA = 27;
	/** Key code constant: Clear key. */
	public static final int KEYCODE_CLEAR = 28;
	/** Key code constant: 'A' key. */
	public static final int KEYCODE_A = 29;
	/** Key code constant: 'B' key. */
	public static final int KEYCODE_B = 30;
	/** Key code constant: 'C' key. */
	public static final int KEYCODE_C = 31;
	/** Key code constant: 'D' key. */
	public static final int KEYCODE_D = 32;
	/** Key code constant: 'E' key. */
	public static final int KEYCODE_E = 33;
	/** Key code constant: 'F' key. */
	public static final int KEYCODE_F = 34;
	/** Key code constant: 'G' key. */
	public static final int KEYCODE_G = 35;
	/** Key code constant: 'H' key. */
	public static final int KEYCODE_H = 36;
	/** Key code constant: 'I' key. */
	public static final int KEYCODE_I = 37;
	/** Key code constant: 'J' key. */
	public static final int KEYCODE_J = 38;
	/** Key code constant: 'K' key. */
	public static final int KEYCODE_K = 39;
	/** Key code constant: 'L' key. */
	public static final int KEYCODE_L = 40;
	/** Key code constant: 'M' key. */
	public static final int KEYCODE_M = 41;
	/** Key code constant: 'N' key. */
	public static final int KEYCODE_N = 42;
	/** Key code constant: 'O' key. */
	public static final int KEYCODE_O = 43;
	/** Key code constant: 'P' key. */
	public static final int KEYCODE_P = 44;
	/** Key code constant: 'Q' key. */
	public static final int KEYCODE_Q = 45;
	/** Key code constant: 'R' key. */
	public static final int KEYCODE_R = 46;
	/** Key code constant: 'S' key. */
	public static final int KEYCODE_S = 47;
	/** Key code constant: 'T' key. */
	public static final int KEYCODE_T = 48;
	/** Key code constant: 'U' key. */
	public static final int KEYCODE_U = 49;
	/** Key code constant: 'V' key. */
	public static final int KEYCODE_V = 50;
	/** Key code constant: 'W' key. */
	public static final int KEYCODE_W = 51;
	/** Key code constant: 'X' key. */
	public static final int KEYCODE_X = 52;
	/** Key code constant: 'Y' key. */
	public static final int KEYCODE_Y = 53;
	/** Key code constant: 'Z' key. */
	public static final int KEYCODE_Z = 54;
	/** Key code constant: ',' key. */
	public static final int KEYCODE_COMMA = 55;
	/** Key code constant: '.' key. */
	public static final int KEYCODE_PERIOD = 56;
	/** Key code constant: Left Alt modifier key. */
	public static final int KEYCODE_ALT_LEFT = 57;
	/** Key code constant: Right Alt modifier key. */
	public static final int KEYCODE_ALT_RIGHT = 58;
	/** Key code constant: Left Shift modifier key. */
	public static final int KEYCODE_SHIFT_LEFT = 59;
	/** Key code constant: Right Shift modifier key. */
	public static final int KEYCODE_SHIFT_RIGHT = 60;
	/** Key code constant: Tab key. */
	public static final int KEYCODE_TAB = 61;
	/** Key code constant: Space key. */
	public static final int KEYCODE_SPACE = 62;
	/**
	 * Key code constant: Symbol modifier key. Used to enter alternate symbols.
	 */
	public static final int KEYCODE_SYM = 63;
	/**
	 * Key code constant: Explorer special function key. Used to launch a
	 * browser application.
	 */
	public static final int KEYCODE_EXPLORER = 64;
	/**
	 * Key code constant: Envelope special function key. Used to launch a mail
	 * application.
	 */
	public static final int KEYCODE_ENVELOPE = 65;
	/** Key code constant: Enter key. */
	public static final int KEYCODE_ENTER = 66;
	/**
	 * Key code constant: Backspace key. Deletes characters before the insertion
	 * point, unlike {@link #KEYCODE_FORWARD_DEL}.
	 */
	public static final int KEYCODE_DEL = 67;
	/** Key code constant: '`' (backtick) key. */
	public static final int KEYCODE_GRAVE = 68;
	/** Key code constant: '-'. */
	public static final int KEYCODE_MINUS = 69;
	/** Key code constant: '=' key. */
	public static final int KEYCODE_EQUALS = 70;
	/** Key code constant: '[' key. */
	public static final int KEYCODE_LEFT_BRACKET = 71;
	/** Key code constant: ']' key. */
	public static final int KEYCODE_RIGHT_BRACKET = 72;
	/** Key code constant: '\' key. */
	public static final int KEYCODE_BACKSLASH = 73;
	/** Key code constant: ';' key. */
	public static final int KEYCODE_SEMICOLON = 74;
	/** Key code constant: ''' (apostrophe) key. */
	public static final int KEYCODE_APOSTROPHE = 75;
	/** Key code constant: '/' key. */
	public static final int KEYCODE_SLASH = 76;
	/** Key code constant: '@' key. */
	public static final int KEYCODE_AT = 77;
	/**
	 * Key code constant: Number modifier key. Used to enter numeric symbols.
	 * This key is not Num Lock; it is more like {@link #KEYCODE_ALT_LEFT} and
	 * is interpreted as an ALT key by
	 * {@link android.text.method.MetaKeyKeyListener}.
	 */
	public static final int KEYCODE_NUM = 78;
	/**
	 * Key code constant: Headset Hook key. Used to hang up calls and stop
	 * media.
	 */
	public static final int KEYCODE_HEADSETHOOK = 79;
	/**
	 * Key code constant: Camera Focus key. Used to focus the camera.
	 */
	public static final int KEYCODE_FOCUS = 80; // *Camera* focus
	/** Key code constant: '+' key. */
	public static final int KEYCODE_PLUS = 81;
	/** Key code constant: Menu key. */
	public static final int KEYCODE_MENU = 82;
	/** Key code constant: Notification key. */
	public static final int KEYCODE_NOTIFICATION = 83;
	/** Key code constant: Search key. */
	public static final int KEYCODE_SEARCH = 84;
	/** Key code constant: Play/Pause media key. */
	public static final int KEYCODE_MEDIA_PLAY_PAUSE = 85;
	/** Key code constant: Stop media key. */
	public static final int KEYCODE_MEDIA_STOP = 86;
	/** Key code constant: Play Next media key. */
	public static final int KEYCODE_MEDIA_NEXT = 87;
	/** Key code constant: Play Previous media key. */
	public static final int KEYCODE_MEDIA_PREVIOUS = 88;
	/** Key code constant: Rewind media key. */
	public static final int KEYCODE_MEDIA_REWIND = 89;
	/** Key code constant: Fast Forward media key. */
	public static final int KEYCODE_MEDIA_FAST_FORWARD = 90;
	/**
	 * Key code constant: Mute key. Mutes the microphone, unlike
	 * {@link #KEYCODE_VOLUME_MUTE}.
	 */
	public static final int KEYCODE_MUTE = 91;
	/** Key code constant: Page Up key. */
	public static final int KEYCODE_PAGE_UP = 92;
	/** Key code constant: Page Down key. */
	public static final int KEYCODE_PAGE_DOWN = 93;
	/**
	 * Key code constant: Picture Symbols modifier key. Used to switch symbol
	 * sets (Emoji, Kao-moji).
	 */
	public static final int KEYCODE_PICTSYMBOLS = 94; // switch symbol-sets
														// (Emoji,Kao-moji)
	/**
	 * Key code constant: Switch Charset modifier key. Used to switch character
	 * sets (Kanji, Katakana).
	 */
	public static final int KEYCODE_SWITCH_CHARSET = 95; // switch char-sets
															// (Kanji,Katakana)
	/**
	 * Key code constant: A Button key. On a game controller, the A button
	 * should be either the button labeled A or the first button on the upper
	 * row of controller buttons.
	 */
	public static final int KEYCODE_BUTTON_A = 96;
	/**
	 * Key code constant: B Button key. On a game controller, the B button
	 * should be either the button labeled B or the second button on the upper
	 * row of controller buttons.
	 */
	public static final int KEYCODE_BUTTON_B = 97;
	/**
	 * Key code constant: C Button key. On a game controller, the C button
	 * should be either the button labeled C or the third button on the upper
	 * row of controller buttons.
	 */
	public static final int KEYCODE_BUTTON_C = 98;
	/**
	 * Key code constant: X Button key. On a game controller, the X button
	 * should be either the button labeled X or the first button on the lower
	 * row of controller buttons.
	 */
	public static final int KEYCODE_BUTTON_X = 99;
	/**
	 * Key code constant: Y Button key. On a game controller, the Y button
	 * should be either the button labeled Y or the second button on the lower
	 * row of controller buttons.
	 */
	public static final int KEYCODE_BUTTON_Y = 100;
	/**
	 * Key code constant: Z Button key. On a game controller, the Z button
	 * should be either the button labeled Z or the third button on the lower
	 * row of controller buttons.
	 */
	public static final int KEYCODE_BUTTON_Z = 101;
	/**
	 * Key code constant: L1 Button key. On a game controller, the L1 button
	 * should be either the button labeled L1 (or L) or the top left trigger
	 * button.
	 */
	public static final int KEYCODE_BUTTON_L1 = 102;
	/**
	 * Key code constant: R1 Button key. On a game controller, the R1 button
	 * should be either the button labeled R1 (or R) or the top right trigger
	 * button.
	 */
	public static final int KEYCODE_BUTTON_R1 = 103;
	/**
	 * Key code constant: L2 Button key. On a game controller, the L2 button
	 * should be either the button labeled L2 or the bottom left trigger button.
	 */
	public static final int KEYCODE_BUTTON_L2 = 104;
	/**
	 * Key code constant: R2 Button key. On a game controller, the R2 button
	 * should be either the button labeled R2 or the bottom right trigger
	 * button.
	 */
	public static final int KEYCODE_BUTTON_R2 = 105;
	/**
	 * Key code constant: Left Thumb Button key. On a game controller, the left
	 * thumb button indicates that the left (or only) joystick is pressed.
	 */
	public static final int KEYCODE_BUTTON_THUMBL = 106;
	/**
	 * Key code constant: Right Thumb Button key. On a game controller, the
	 * right thumb button indicates that the right joystick is pressed.
	 */
	public static final int KEYCODE_BUTTON_THUMBR = 107;
	/**
	 * Key code constant: Start Button key. On a game controller, the button
	 * labeled Start.
	 */
	public static final int KEYCODE_BUTTON_START = 108;
	/**
	 * Key code constant: Select Button key. On a game controller, the button
	 * labeled Select.
	 */
	public static final int KEYCODE_BUTTON_SELECT = 109;
	/**
	 * Key code constant: Mode Button key. On a game controller, the button
	 * labeled Mode.
	 */
	public static final int KEYCODE_BUTTON_MODE = 110;
	/** Key code constant: Escape key. */
	public static final int KEYCODE_ESCAPE = 111;
	/**
	 * Key code constant: Forward Delete key. Deletes characters ahead of the
	 * insertion point, unlike {@link #KEYCODE_DEL}.
	 */
	public static final int KEYCODE_FORWARD_DEL = 112;
	/** Key code constant: Left Control modifier key. */
	public static final int KEYCODE_CTRL_LEFT = 113;
	/** Key code constant: Right Control modifier key. */
	public static final int KEYCODE_CTRL_RIGHT = 114;
	/** Key code constant: Caps Lock key. */
	public static final int KEYCODE_CAPS_LOCK = 115;
	/** Key code constant: Scroll Lock key. */
	public static final int KEYCODE_SCROLL_LOCK = 116;
	/** Key code constant: Left Meta modifier key. */
	public static final int KEYCODE_META_LEFT = 117;
	/** Key code constant: Right Meta modifier key. */
	public static final int KEYCODE_META_RIGHT = 118;
	/** Key code constant: Function modifier key. */
	public static final int KEYCODE_FUNCTION = 119;
	/** Key code constant: System Request / Print Screen key. */
	public static final int KEYCODE_SYSRQ = 120;
	/** Key code constant: Break / Pause key. */
	public static final int KEYCODE_BREAK = 121;
	/**
	 * Key code constant: Home Movement key. Used for scrolling or moving the
	 * cursor around to the start of a line or to the top of a list.
	 */
	public static final int KEYCODE_MOVE_HOME = 122;
	/**
	 * Key code constant: End Movement key. Used for scrolling or moving the
	 * cursor around to the end of a line or to the bottom of a list.
	 */
	public static final int KEYCODE_MOVE_END = 123;
	/**
	 * Key code constant: Insert key. Toggles insert / overwrite edit mode.
	 */
	public static final int KEYCODE_INSERT = 124;
	/**
	 * Key code constant: Forward key. Navigates forward in the history stack.
	 * Complement of {@link #KEYCODE_BACK}.
	 */
	public static final int KEYCODE_FORWARD = 125;
	/** Key code constant: Play media key. */
	public static final int KEYCODE_MEDIA_PLAY = 126;
	/** Key code constant: Pause media key. */
	public static final int KEYCODE_MEDIA_PAUSE = 127;
	/**
	 * Key code constant: Close media key. May be used to close a CD tray, for
	 * example.
	 */
	public static final int KEYCODE_MEDIA_CLOSE = 128;
	/**
	 * Key code constant: Eject media key. May be used to eject a CD tray, for
	 * example.
	 */
	public static final int KEYCODE_MEDIA_EJECT = 129;
	/** Key code constant: Record media key. */
	public static final int KEYCODE_MEDIA_RECORD = 130;
	/** Key code constant: F1 key. */
	public static final int KEYCODE_F1 = 131;
	/** Key code constant: F2 key. */
	public static final int KEYCODE_F2 = 132;
	/** Key code constant: F3 key. */
	public static final int KEYCODE_F3 = 133;
	/** Key code constant: F4 key. */
	public static final int KEYCODE_F4 = 134;
	/** Key code constant: F5 key. */
	public static final int KEYCODE_F5 = 135;
	/** Key code constant: F6 key. */
	public static final int KEYCODE_F6 = 136;
	/** Key code constant: F7 key. */
	public static final int KEYCODE_F7 = 137;
	/** Key code constant: F8 key. */
	public static final int KEYCODE_F8 = 138;
	/** Key code constant: F9 key. */
	public static final int KEYCODE_F9 = 139;
	/** Key code constant: F10 key. */
	public static final int KEYCODE_F10 = 140;
	/** Key code constant: F11 key. */
	public static final int KEYCODE_F11 = 141;
	/** Key code constant: F12 key. */
	public static final int KEYCODE_F12 = 142;
	/**
	 * Key code constant: Num Lock key. This is the Num Lock key; it is
	 * different from {@link #KEYCODE_NUM}. This key alters the behavior of
	 * other keys on the numeric keypad.
	 */
	public static final int KEYCODE_NUM_LOCK = 143;
	/** Key code constant: Numeric keypad '0' key. */
	public static final int KEYCODE_NUMPAD_0 = 144;
	/** Key code constant: Numeric keypad '1' key. */
	public static final int KEYCODE_NUMPAD_1 = 145;
	/** Key code constant: Numeric keypad '2' key. */
	public static final int KEYCODE_NUMPAD_2 = 146;
	/** Key code constant: Numeric keypad '3' key. */
	public static final int KEYCODE_NUMPAD_3 = 147;
	/** Key code constant: Numeric keypad '4' key. */
	public static final int KEYCODE_NUMPAD_4 = 148;
	/** Key code constant: Numeric keypad '5' key. */
	public static final int KEYCODE_NUMPAD_5 = 149;
	/** Key code constant: Numeric keypad '6' key. */
	public static final int KEYCODE_NUMPAD_6 = 150;
	/** Key code constant: Numeric keypad '7' key. */
	public static final int KEYCODE_NUMPAD_7 = 151;
	/** Key code constant: Numeric keypad '8' key. */
	public static final int KEYCODE_NUMPAD_8 = 152;
	/** Key code constant: Numeric keypad '9' key. */
	public static final int KEYCODE_NUMPAD_9 = 153;
	/** Key code constant: Numeric keypad '/' key (for division). */
	public static final int KEYCODE_NUMPAD_DIVIDE = 154;
	/** Key code constant: Numeric keypad '*' key (for multiplication). */
	public static final int KEYCODE_NUMPAD_MULTIPLY = 155;
	/** Key code constant: Numeric keypad '-' key (for subtraction). */
	public static final int KEYCODE_NUMPAD_SUBTRACT = 156;
	/** Key code constant: Numeric keypad '+' key (for addition). */
	public static final int KEYCODE_NUMPAD_ADD = 157;
	/**
	 * Key code constant: Numeric keypad '.' key (for decimals or digit
	 * grouping).
	 */
	public static final int KEYCODE_NUMPAD_DOT = 158;
	/**
	 * Key code constant: Numeric keypad ',' key (for decimals or digit
	 * grouping).
	 */
	public static final int KEYCODE_NUMPAD_COMMA = 159;
	/** Key code constant: Numeric keypad Enter key. */
	public static final int KEYCODE_NUMPAD_ENTER = 160;
	/** Key code constant: Numeric keypad '=' key. */
	public static final int KEYCODE_NUMPAD_EQUALS = 161;
	/** Key code constant: Numeric keypad '(' key. */
	public static final int KEYCODE_NUMPAD_LEFT_PAREN = 162;
	/** Key code constant: Numeric keypad ')' key. */
	public static final int KEYCODE_NUMPAD_RIGHT_PAREN = 163;
	/**
	 * Key code constant: Volume Mute key. Mutes the speaker, unlike
	 * {@link #KEYCODE_MUTE}. This key should normally be implemented as a
	 * toggle such that the first press mutes the speaker and the second press
	 * restores the original volume.
	 */
	public static final int KEYCODE_VOLUME_MUTE = 164;
	/**
	 * Key code constant: Info key. Common on TV remotes to show additional
	 * information related to what is currently being viewed.
	 */
	public static final int KEYCODE_INFO = 165;
	/**
	 * Key code constant: Channel up key. On TV remotes, increments the
	 * television channel.
	 */
	public static final int KEYCODE_CHANNEL_UP = 166;
	/**
	 * Key code constant: Channel down key. On TV remotes, decrements the
	 * television channel.
	 */
	public static final int KEYCODE_CHANNEL_DOWN = 167;
	/** Key code constant: Zoom in key. */
	public static final int KEYCODE_ZOOM_IN = 168;
	/** Key code constant: Zoom out key. */
	public static final int KEYCODE_ZOOM_OUT = 169;
	/**
	 * Key code constant: TV key. On TV remotes, switches to viewing live TV.
	 */
	public static final int KEYCODE_TV = 170;
	/**
	 * Key code constant: Window key. On TV remotes, toggles picture-in-picture
	 * mode or other windowing functions.
	 */
	public static final int KEYCODE_WINDOW = 171;
	/**
	 * Key code constant: Guide key. On TV remotes, shows a programming guide.
	 */
	public static final int KEYCODE_GUIDE = 172;
	/**
	 * Key code constant: DVR key. On some TV remotes, switches to a DVR mode
	 * for recorded shows.
	 */
	public static final int KEYCODE_DVR = 173;
	/**
	 * Key code constant: Bookmark key. On some TV remotes, bookmarks content or
	 * web pages.
	 */
	public static final int KEYCODE_BOOKMARK = 174;
	/**
	 * Key code constant: Toggle captions key. Switches the mode for
	 * closed-captioning text, for example during television shows.
	 */
	public static final int KEYCODE_CAPTIONS = 175;
	/**
	 * Key code constant: Settings key. Starts the system settings activity.
	 */
	public static final int KEYCODE_SETTINGS = 176;
	/**
	 * Key code constant: TV power key. On TV remotes, toggles the power on a
	 * television screen.
	 */
	public static final int KEYCODE_TV_POWER = 177;
	/**
	 * Key code constant: TV input key. On TV remotes, switches the input on a
	 * television screen.
	 */
	public static final int KEYCODE_TV_INPUT = 178;
	/**
	 * Key code constant: Set-top-box power key. On TV remotes, toggles the
	 * power on an external Set-top-box.
	 */
	public static final int KEYCODE_STB_POWER = 179;
	/**
	 * Key code constant: Set-top-box input key. On TV remotes, switches the
	 * input mode on an external Set-top-box.
	 */
	public static final int KEYCODE_STB_INPUT = 180;
	/**
	 * Key code constant: A/V Receiver power key. On TV remotes, toggles the
	 * power on an external A/V Receiver.
	 */
	public static final int KEYCODE_AVR_POWER = 181;
	/**
	 * Key code constant: A/V Receiver input key. On TV remotes, switches the
	 * input mode on an external A/V Receiver.
	 */
	public static final int KEYCODE_AVR_INPUT = 182;
	/**
	 * Key code constant: Red "programmable" key. On TV remotes, acts as a
	 * contextual/programmable key.
	 */
	public static final int KEYCODE_PROG_RED = 183;
	/**
	 * Key code constant: Green "programmable" key. On TV remotes, actsas a
	 * contextual/programmable key.
	 */
	public static final int KEYCODE_PROG_GREEN = 184;
	/**
	 * Key code constant: Yellow "programmable" key. On TV remotes, acts as a
	 * contextual/programmable key.
	 */
	public static final int KEYCODE_PROG_YELLOW = 185;
	/**
	 * Key code constant: Blue "programmable" key. On TV remotes, acts as a
	 * contextual/programmable key.
	 */
	public static final int KEYCODE_PROG_BLUE = 186;
	/**
	 * Key code constant: App switch key. Should bring up the application
	 * switcher dialog.
	 */
	public static final int KEYCODE_APP_SWITCH = 187;
	/** Key code constant: Generic Game Pad Button #1. */
	public static final int KEYCODE_BUTTON_1 = 188;
	/** Key code constant: Generic Game Pad Button #2. */
	public static final int KEYCODE_BUTTON_2 = 189;
	/** Key code constant: Generic Game Pad Button #3. */
	public static final int KEYCODE_BUTTON_3 = 190;
	/** Key code constant: Generic Game Pad Button #4. */
	public static final int KEYCODE_BUTTON_4 = 191;
	/** Key code constant: Generic Game Pad Button #5. */
	public static final int KEYCODE_BUTTON_5 = 192;
	/** Key code constant: Generic Game Pad Button #6. */
	public static final int KEYCODE_BUTTON_6 = 193;
	/** Key code constant: Generic Game Pad Button #7. */
	public static final int KEYCODE_BUTTON_7 = 194;
	/** Key code constant: Generic Game Pad Button #8. */
	public static final int KEYCODE_BUTTON_8 = 195;
	/** Key code constant: Generic Game Pad Button #9. */
	public static final int KEYCODE_BUTTON_9 = 196;
	/** Key code constant: Generic Game Pad Button #10. */
	public static final int KEYCODE_BUTTON_10 = 197;
	/** Key code constant: Generic Game Pad Button #11. */
	public static final int KEYCODE_BUTTON_11 = 198;
	/** Key code constant: Generic Game Pad Button #12. */
	public static final int KEYCODE_BUTTON_12 = 199;
	/** Key code constant: Generic Game Pad Button #13. */
	public static final int KEYCODE_BUTTON_13 = 200;
	/** Key code constant: Generic Game Pad Button #14. */
	public static final int KEYCODE_BUTTON_14 = 201;
	/** Key code constant: Generic Game Pad Button #15. */
	public static final int KEYCODE_BUTTON_15 = 202;
	/** Key code constant: Generic Game Pad Button #16. */
	public static final int KEYCODE_BUTTON_16 = 203;
	/**
	 * Key code constant: Language Switch key. Toggles the current input
	 * language such as switching between English and Japanese on a QWERTY
	 * keyboard. On some devices, the same function may be performed by pressing
	 * Shift+Spacebar.
	 */
	public static final int KEYCODE_LANGUAGE_SWITCH = 204;
	/**
	 * Key code constant: Manner Mode key. Toggles silent or vibrate mode on and
	 * off to make the device behave more politely in certain settings such as
	 * on a crowded train. On some devices, the key may only operate when
	 * long-pressed.
	 */
	public static final int KEYCODE_MANNER_MODE = 205;
	/**
	 * Key code constant: 3D Mode key. Toggles the display between 2D and 3D
	 * mode.
	 */
	public static final int KEYCODE_3D_MODE = 206;
	/**
	 * Key code constant: Contacts special function key. Used to launch an
	 * address book application.
	 */
	public static final int KEYCODE_CONTACTS = 207;
	/**
	 * Key code constant: Calendar special function key. Used to launch a
	 * calendar application.
	 */
	public static final int KEYCODE_CALENDAR = 208;
	/**
	 * Key code constant: Music special function key. Used to launch a music
	 * player application.
	 */
	public static final int KEYCODE_MUSIC = 209;
	/**
	 * Key code constant: Calculator special function key. Used to launch a
	 * calculator application.
	 */
	public static final int KEYCODE_CALCULATOR = 210;
	/** Key code constant: Japanese full-width / half-width key. */
	public static final int KEYCODE_ZENKAKU_HANKAKU = 211;
	/** Key code constant: Japanese alphanumeric key. */
	public static final int KEYCODE_EISU = 212;
	/** Key code constant: Japanese non-conversion key. */
	public static final int KEYCODE_MUHENKAN = 213;
	/** Key code constant: Japanese conversion key. */
	public static final int KEYCODE_HENKAN = 214;
	/** Key code constant: Japanese katakana / hiragana key. */
	public static final int KEYCODE_KATAKANA_HIRAGANA = 215;
	/** Key code constant: Japanese Yen key. */
	public static final int KEYCODE_YEN = 216;
	/** Key code constant: Japanese Ro key. */
	public static final int KEYCODE_RO = 217;
	/** Key code constant: Japanese kana key. */
	public static final int KEYCODE_KANA = 218;
	/**
	 * Key code constant: Assist key. Launches the global assist activity. Not
	 * delivered to applications.
	 */
	public static final int KEYCODE_ASSIST = 219;

	/**
	 * SHIFT key locked in CAPS mode. Reserved for use by
	 * {@link MetaKeyKeyListener} for a published constant in its API.
	 * 
	 * @hide
	 */
	public static final int META_CAP_LOCKED = 0x100;

	/**
	 * ALT key locked. Reserved for use by {@link MetaKeyKeyListener} for a
	 * published constant in its API.
	 * 
	 * @hide
	 */
	public static final int META_ALT_LOCKED = 0x200;

	/**
	 * SYM key locked. Reserved for use by {@link MetaKeyKeyListener} for a
	 * published constant in its API.
	 * 
	 * @hide
	 */
	public static final int META_SYM_LOCKED = 0x400;

	/**
	 * Text is in selection mode. Reserved for use by {@link MetaKeyKeyListener}
	 * for a private unpublished constant in its API that is currently being
	 * retained for legacy reasons.
	 * 
	 * @hide
	 */
	public static final int META_SELECTING = 0x800;

	/**
	 * <p>
	 * This mask is used to check whether one of the ALT meta keys is pressed.
	 * </p>
	 * 
	 * @see #isAltPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_ALT_LEFT
	 * @see #KEYCODE_ALT_RIGHT
	 */
	public static final int META_ALT_ON = 0x02;

	/**
	 * <p>
	 * This mask is used to check whether the left ALT meta key is pressed.
	 * </p>
	 * 
	 * @see #isAltPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_ALT_LEFT
	 */
	public static final int META_ALT_LEFT_ON = 0x10;

	/**
	 * <p>
	 * This mask is used to check whether the right the ALT meta key is pressed.
	 * </p>
	 * 
	 * @see #isAltPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_ALT_RIGHT
	 */
	public static final int META_ALT_RIGHT_ON = 0x20;

	/**
	 * <p>
	 * This mask is used to check whether one of the SHIFT meta keys is pressed.
	 * </p>
	 * 
	 * @see #isShiftPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_SHIFT_LEFT
	 * @see #KEYCODE_SHIFT_RIGHT
	 */
	public static final int META_SHIFT_ON = 0x1;

	/**
	 * <p>
	 * This mask is used to check whether the left SHIFT meta key is pressed.
	 * </p>
	 * 
	 * @see #isShiftPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_SHIFT_LEFT
	 */
	public static final int META_SHIFT_LEFT_ON = 0x40;

	/**
	 * <p>
	 * This mask is used to check whether the right SHIFT meta key is pressed.
	 * </p>
	 * 
	 * @see #isShiftPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_SHIFT_RIGHT
	 */
	public static final int META_SHIFT_RIGHT_ON = 0x80;

	/**
	 * <p>
	 * This mask is used to check whether the SYM meta key is pressed.
	 * </p>
	 * 
	 * @see #isSymPressed()
	 * @see #getMetaState()
	 */
	public static final int META_SYM_ON = 0x4;

	/**
	 * <p>
	 * This mask is used to check whether the FUNCTION meta key is pressed.
	 * </p>
	 * 
	 * @see #isFunctionPressed()
	 * @see #getMetaState()
	 */
	public static final int META_FUNCTION_ON = 0x8;

	/**
	 * <p>
	 * This mask is used to check whether one of the CTRL meta keys is pressed.
	 * </p>
	 * 
	 * @see #isCtrlPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_CTRL_LEFT
	 * @see #KEYCODE_CTRL_RIGHT
	 */
	public static final int META_CTRL_ON = 0x1000;

	/**
	 * <p>
	 * This mask is used to check whether the left CTRL meta key is pressed.
	 * </p>
	 * 
	 * @see #isCtrlPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_CTRL_LEFT
	 */
	public static final int META_CTRL_LEFT_ON = 0x2000;

	/**
	 * <p>
	 * This mask is used to check whether the right CTRL meta key is pressed.
	 * </p>
	 * 
	 * @see #isCtrlPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_CTRL_RIGHT
	 */
	public static final int META_CTRL_RIGHT_ON = 0x4000;

	/**
	 * <p>
	 * This mask is used to check whether one of the META meta keys is pressed.
	 * </p>
	 * 
	 * @see #isMetaPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_META_LEFT
	 * @see #KEYCODE_META_RIGHT
	 */
	public static final int META_META_ON = 0x10000;

	/**
	 * <p>
	 * This mask is used to check whether the left META meta key is pressed.
	 * </p>
	 * 
	 * @see #isMetaPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_META_LEFT
	 */
	public static final int META_META_LEFT_ON = 0x20000;

	/**
	 * <p>
	 * This mask is used to check whether the right META meta key is pressed.
	 * </p>
	 * 
	 * @see #isMetaPressed()
	 * @see #getMetaState()
	 * @see #KEYCODE_META_RIGHT
	 */
	public static final int META_META_RIGHT_ON = 0x40000;

	/**
	 * <p>
	 * This mask is used to check whether the CAPS LOCK meta key is on.
	 * </p>
	 * 
	 * @see #isCapsLockOn()
	 * @see #getMetaState()
	 * @see #KEYCODE_CAPS_LOCK
	 */
	public static final int META_CAPS_LOCK_ON = 0x100000;

	/**
	 * <p>
	 * This mask is used to check whether the NUM LOCK meta key is on.
	 * </p>
	 * 
	 * @see #isNumLockOn()
	 * @see #getMetaState()
	 * @see #KEYCODE_NUM_LOCK
	 */
	public static final int META_NUM_LOCK_ON = 0x200000;

	/**
	 * <p>
	 * This mask is used to check whether the SCROLL LOCK meta key is on.
	 * </p>
	 * 
	 * @see #isScrollLockOn()
	 * @see #getMetaState()
	 * @see #KEYCODE_SCROLL_LOCK
	 */
	public static final int META_SCROLL_LOCK_ON = 0x400000;

	/**
	 * This mask is a combination of {@link #META_SHIFT_ON},
	 * {@link #META_SHIFT_LEFT_ON} and {@link #META_SHIFT_RIGHT_ON}.
	 */
	public static final int META_SHIFT_MASK = META_SHIFT_ON
			| META_SHIFT_LEFT_ON | META_SHIFT_RIGHT_ON;

	/**
	 * This mask is a combination of {@link #META_ALT_ON},
	 * {@link #META_ALT_LEFT_ON} and {@link #META_ALT_RIGHT_ON}.
	 */
	public static final int META_ALT_MASK = META_ALT_ON | META_ALT_LEFT_ON
			| META_ALT_RIGHT_ON;

	/**
	 * This mask is a combination of {@link #META_CTRL_ON},
	 * {@link #META_CTRL_LEFT_ON} and {@link #META_CTRL_RIGHT_ON}.
	 */
	public static final int META_CTRL_MASK = META_CTRL_ON | META_CTRL_LEFT_ON
			| META_CTRL_RIGHT_ON;

	/**
	 * This mask is a combination of {@link #META_META_ON},
	 * {@link #META_META_LEFT_ON} and {@link #META_META_RIGHT_ON}.
	 */
	public static final int META_META_MASK = META_META_ON | META_META_LEFT_ON
			| META_META_RIGHT_ON;

	public static final SparseArray<byte[]> KeyBinding = new SparseArray<byte[]>();
	static
	{
		KeyBinding.put(KEYCODE_ESCAPE, 		new byte[]{ ControlCharacter.ESC });
		KeyBinding.put(KEYCODE_DEL, 		new byte[]{ ControlCharacter.DEL });
		KeyBinding.put(KEYCODE_TAB, 		new byte[]{ ControlCharacter.HT });
		KeyBinding.put(KEYCODE_ENTER, 		new byte[]{ ControlCharacter.LF });
		KeyBinding.put(KEYCODE_DPAD_CENTER,	new byte[]{ ControlCharacter.LF });
		KeyBinding.put(KEYCODE_DPAD_UP, 	new byte[]{ ControlCharacter.ESC, '[', 'A' });
		KeyBinding.put(KEYCODE_DPAD_DOWN, 	new byte[]{ ControlCharacter.ESC, '[', 'B' });
		KeyBinding.put(KEYCODE_DPAD_LEFT, 	new byte[]{ ControlCharacter.ESC, '[', 'D' });
		KeyBinding.put(KEYCODE_DPAD_RIGHT, 	new byte[]{ ControlCharacter.ESC, '[', 'C' });
		KeyBinding.put(KEYCODE_MOVE_HOME, 	new byte[]{ ControlCharacter.ESC, '[', '1', '~' });
		KeyBinding.put(KEYCODE_MOVE_END, 	new byte[]{ ControlCharacter.ESC, '[', '4', '~' });
		KeyBinding.put(KEYCODE_PAGE_UP, 	new byte[]{ ControlCharacter.ESC, '[', '5', '~' });
		KeyBinding.put(KEYCODE_PAGE_DOWN, 	new byte[]{ ControlCharacter.ESC, '[', '6', '~' });
		KeyBinding.put(KEYCODE_INSERT, 		new byte[]{ ControlCharacter.ESC, '[', '2',	'~' });
		KeyBinding.put(KEYCODE_FORWARD_DEL, new byte[]{ ControlCharacter.ESC, '[', '3',	'~' });
		KeyBinding.put(KEYCODE_SYSRQ, 		new byte[]{ ControlCharacter.ESC, '[', '3',	'2', '~' });
		KeyBinding.put(KEYCODE_BREAK, 		new byte[]{ ControlCharacter.ESC, '[', '3',	'4', '~' });
		KeyBinding.put(KEYCODE_NUM_LOCK,	new byte[]{ ControlCharacter.ESC, 'O', 'P' });
		
		KeyBinding.put(KEYCODE_NUMPAD_0, 	new byte[]{ '0' });
		KeyBinding.put(KEYCODE_NUMPAD_1, 	new byte[]{ '1' });
		KeyBinding.put(KEYCODE_NUMPAD_2, 	new byte[]{ '2' });
		KeyBinding.put(KEYCODE_NUMPAD_3, 	new byte[]{ '3' });
		KeyBinding.put(KEYCODE_NUMPAD_4, 	new byte[]{ '4' });
		KeyBinding.put(KEYCODE_NUMPAD_5, 	new byte[]{ '5' });
		KeyBinding.put(KEYCODE_NUMPAD_6, 	new byte[]{ '6' });
		KeyBinding.put(KEYCODE_NUMPAD_7, 	new byte[]{ '7' });
		KeyBinding.put(KEYCODE_NUMPAD_8, 	new byte[]{ '8' });
		KeyBinding.put(KEYCODE_NUMPAD_9, 	new byte[]{ '9' });
		KeyBinding.put(KEYCODE_NUMPAD_DIVIDE, 		new byte[]{ '/' });
		KeyBinding.put(KEYCODE_NUMPAD_MULTIPLY, 	new byte[]{ '*' });
		KeyBinding.put(KEYCODE_NUMPAD_SUBTRACT, 	new byte[]{ '-' });
		KeyBinding.put(KEYCODE_NUMPAD_ADD, 			new byte[]{ '+' });
		KeyBinding.put(KEYCODE_NUMPAD_ENTER,		new byte[]{ ControlCharacter.LF });
		KeyBinding.put(KEYCODE_NUMPAD_EQUALS,		new byte[]{ '=' });
		KeyBinding.put(KEYCODE_NUMPAD_DOT,			new byte[]{ '.' });
		KeyBinding.put(KEYCODE_NUMPAD_COMMA,		new byte[]{ ',' });
		
		KeyBinding.put(KEYCODE_F1,					new byte[]{ ControlCharacter.ESC, 'O', 'P' });
		KeyBinding.put(KEYCODE_F2,					new byte[]{ ControlCharacter.ESC, 'O', 'Q' });
		KeyBinding.put(KEYCODE_F3,					new byte[]{ ControlCharacter.ESC, 'O', 'R' });
		KeyBinding.put(KEYCODE_F4,					new byte[]{ ControlCharacter.ESC, 'O', 'S' });
		KeyBinding.put(KEYCODE_F5,					new byte[]{ ControlCharacter.ESC, '[', '1', '5', '~' });
		KeyBinding.put(KEYCODE_F6,					new byte[]{ ControlCharacter.ESC, '[', '1', '7', '~' });
		KeyBinding.put(KEYCODE_F7,					new byte[]{ ControlCharacter.ESC, '[', '1', '8', '~' });
		KeyBinding.put(KEYCODE_F8,					new byte[]{ ControlCharacter.ESC, '[', '1', '9', '~' });
		KeyBinding.put(KEYCODE_F9,					new byte[]{ ControlCharacter.ESC, '[', '2', '0', '~' });
		KeyBinding.put(KEYCODE_F10,					new byte[]{ ControlCharacter.ESC, '[', '2', '1', '~' });
		KeyBinding.put(KEYCODE_F11,					new byte[]{ ControlCharacter.ESC, '[', '2', '3', '~' });
		KeyBinding.put(KEYCODE_F12,					new byte[]{ ControlCharacter.ESC, '[', '2', '4', '~' });
	}

	
	private int mSoftMetaState;
	private int mInputMetaState;
	private int mCombiningAccent;
	
	private byte[] mResultBuffer = new byte[16];

	private EmulatorInputListener mListener;
	private HorizontalScrollView mScrollView;
	private LinearLayout mKeys;

	public EmulatorInput(Context context) {
		super(context);
		initializeView();
	}

	public EmulatorInput(Context context, AttributeSet attrs) {
		super(context, attrs);
		initializeView();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public EmulatorInput(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initializeView();
	}

	void initializeView() {

		setFocusable(true);
		setFocusableInTouchMode(true);

		mKeys = new LinearLayout(getContext());
		mScrollView = new HorizontalScrollView(getContext());
		mScrollView.setHorizontalScrollBarEnabled(false);
		mScrollView.setVisibility(View.GONE);
		mScrollView.addView(mKeys, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		
		addView(mScrollView, new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));

		addAdditionalKey("Ctrl", META_CTRL_ON, true);
		addAdditionalKey("Esc", KEYCODE_ESCAPE);
		addAdditionalKey("Up", KEYCODE_DPAD_UP);
		addAdditionalKey("Down", KEYCODE_DPAD_DOWN);
		addAdditionalKey("Left", KEYCODE_DPAD_LEFT);
		addAdditionalKey("Right", KEYCODE_DPAD_RIGHT);
		addAdditionalKey("PgUp", KEYCODE_PAGE_UP);
		addAdditionalKey("PgDn", KEYCODE_PAGE_DOWN);
		addAdditionalKey("Home", KEYCODE_MOVE_HOME);
		addAdditionalKey("End", KEYCODE_MOVE_END);
		addAdditionalKey("Tab", KEYCODE_TAB);
		addAdditionalKey("Backspace", KEYCODE_DEL);
		addAdditionalKey("Insert", KEYCODE_INSERT);
		addAdditionalKey("Delete", KEYCODE_FORWARD_DEL);
		addAdditionalKey("SysRq", KEYCODE_SYSRQ);
		addAdditionalKey("Break", KEYCODE_BREAK);
		addAdditionalKey("F1", KEYCODE_F1);
		addAdditionalKey("F2", KEYCODE_F2);
		addAdditionalKey("F3", KEYCODE_F3);
		addAdditionalKey("F4", KEYCODE_F4);
		addAdditionalKey("F5", KEYCODE_F5);
		addAdditionalKey("F6", KEYCODE_F6);
		addAdditionalKey("F7", KEYCODE_F7);
		addAdditionalKey("F8", KEYCODE_F8);
		addAdditionalKey("F9", KEYCODE_F9);
		addAdditionalKey("F10", KEYCODE_F10);
		addAdditionalKey("F11", KEYCODE_F11);
		addAdditionalKey("F12", KEYCODE_F12);
	}

	public boolean isAdditionalSoftInputEnabled() {
		return mScrollView.getVisibility() == View.VISIBLE;
	}

	public void setAdditionalSoftInputEnabled(boolean enabled) {
		mScrollView.setVisibility(enabled ? View.VISIBLE : View.GONE);
	}

	public boolean toggleAdditionalSoftInput() {
		mScrollView
				.setVisibility(mScrollView.getVisibility() == View.VISIBLE ? View.GONE
						: View.VISIBLE);
		return mScrollView.getVisibility() == View.VISIBLE;
	}

	public boolean toggleSoftInput() {
		InputMethodManager imm = (InputMethodManager) getContext()
				.getSystemService(Context.INPUT_METHOD_SERVICE);

		imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
		return true;
	}

	void addAdditionalKey(String title, int keyCode) {
		addAdditionalKey(title, keyCode, false);
	}

	void addAdditionalKey(String title, int keyCode, boolean isModifier) {
		if (isModifier) {

			final LayoutInflater inflater = LayoutInflater.from(getContext());
			ToggleButton key = (ToggleButton) inflater.inflate(
					R.layout.softinput_toggle, null);

			key.setText(title);
			key.setTextOff(title);
			key.setTextOn(title);
			key.setTag(keyCode);
			key.setOnCheckedChangeListener(this);

			mKeys.addView(key);

		} else {

			final LayoutInflater inflater = LayoutInflater.from(getContext());
			Button key = (Button) inflater.inflate(R.layout.softinput_button,
					null);

			key.setText(title);
			key.setTag(keyCode);
			key.setOnClickListener(this);

			mKeys.addView(key);
		}
	}

	@Override
	public void onClick(View v) {

		/*
		 * dispatch key event from additional soft input will only handle
		 * non-unicode keys
		 */
		processKey((Integer) v.getTag());
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

		/* set / clear additional metastates */

		Integer metaCode = (Integer) buttonView.getTag();
		if (isChecked) {
			setSoftMetaState(metaCode);
		} else {
			clearSoftMetaState(metaCode);
		}
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {

		if (event.isSystem()) {

			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				if (event.getAction() == KeyEvent.ACTION_UP)
					toggleAdditionalSoftInput();
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (event.getAction() == KeyEvent.ACTION_UP)
					toggleSoftInput();
				return true;
			}

		} else if (processKeyEvent(event)) {
			return true;
		}

		return super.onKeyPreIme(keyCode, event);
	}

	@Override
	public boolean onCheckIsTextEditor() {
		return true;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		outAttrs.inputType = EditorInfo.TYPE_NULL;
		return new BaseInputConnection(this, false) {

			final ExtractedText _extract = new ExtractedText();

			@Override
			public boolean commitText(CharSequence text, int newCursorPosition) {
				for (int i = 0; i < text.length(); ++i)
					processChar(text.charAt(i));

				return true;
			}

			@Override
			public boolean deleteSurroundingText(int beforeLength,
					int afterLength) {

				processKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
						KeyEvent.KEYCODE_DEL));
				processKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
						KeyEvent.KEYCODE_DEL));

				return true;
			}

			@Override
			public boolean performEditorAction(int actionCode) {

				if (actionCode == EditorInfo.IME_ACTION_UNSPECIFIED) {
					processKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
							KeyEvent.KEYCODE_ENTER));
					processKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,
							KeyEvent.KEYCODE_ENTER));

					return true;
				}
				return false;
			}

			@Override
			public boolean sendKeyEvent(KeyEvent event) {
				return processKeyEvent(event);
			}

			@Override
			public boolean setComposingText(CharSequence text,
					int newCursorPosition) {
				return true;
			}

			@Override
			public boolean setSelection(int start, int end) {
				return true;
			}

			@Override
			public ExtractedText getExtractedText(ExtractedTextRequest request,
					int flags) {
				return _extract;
			}
		};
	}

	public void setSoftMetaState(int state) {
		mSoftMetaState |= state;
	}

	public void clearSoftMetaState(int state) {
		mSoftMetaState &= ~state;
	}

	public boolean processKeyEvent(KeyEvent event) {
		/* check if we need to handle this type of key */

		if (event.getAction() == KeyEvent.ACTION_UP)
			processKeyUp(event);

		if (event.getAction() == KeyEvent.ACTION_DOWN)
			processKeyDown(event);

		return true;
	}

	private void processKeyUp(KeyEvent event) {
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			mInputMetaState &= ~(META_ALT_ON);
			break;
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			mInputMetaState &= ~(META_SHIFT_ON);
			break;
		}
	}

	private void processKeyDown(KeyEvent event) {
		/* first handle special keys */
		if (processKey(event.getKeyCode())) {
			return;
		}

		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_ALT_LEFT:
		case KeyEvent.KEYCODE_ALT_RIGHT:
			mInputMetaState |= (KeyEvent.META_ALT_ON);
			break;
		case KeyEvent.KEYCODE_SHIFT_LEFT:
		case KeyEvent.KEYCODE_SHIFT_RIGHT:
			mInputMetaState |= (KeyEvent.META_SHIFT_ON);
			break;
		/* keys to ignore */
		case KeyEvent.KEYCODE_CTRL_LEFT:
		case KeyEvent.KEYCODE_CTRL_RIGHT:
		case KeyEvent.KEYCODE_CAPS_LOCK:
		case KeyEvent.KEYCODE_FUNCTION:
			return;
		default:

			int eventMetaState = event.getMetaState();
			int fullMetaState = eventMetaState & ~(META_CTRL_MASK);

			boolean hasCtrl = ((mSoftMetaState | mInputMetaState | eventMetaState) & META_CTRL_ON) != 0;
			boolean hasCaps = ((mSoftMetaState | mInputMetaState | eventMetaState) & KeyEvent.META_SHIFT_ON) != 0;
			boolean hasAlt = ((mSoftMetaState | mInputMetaState | eventMetaState) & KeyEvent.META_ALT_ON) != 0;
			boolean hasFn = ((mSoftMetaState | mInputMetaState | eventMetaState) & META_FUNCTION_ON) != 0;
			boolean hasMeta = ((mSoftMetaState | mInputMetaState | eventMetaState) & META_META_ON) != 0;

			if (hasCaps) {
				fullMetaState |= KeyEvent.META_SHIFT_ON;
			}

			if (hasAlt || hasMeta) {
				/* alt and meta sends escape */
				processKey(KEYCODE_ESCAPE);

				/* remove alt and meta masks */
				fullMetaState &= ~META_ALT_MASK;
				fullMetaState &= ~META_META_MASK;
			}

			int unicodeChar = event.getUnicodeChar(fullMetaState);
			if ((unicodeChar & KeyCharacterMap.COMBINING_ACCENT) != 0) {
				mCombiningAccent = unicodeChar
						& KeyCharacterMap.COMBINING_ACCENT_MASK;
				return;
			}
			if (mCombiningAccent != 0) {
				unicodeChar = KeyCharacterMap.getDeadChar(mCombiningAccent,
						unicodeChar);
				mCombiningAccent = 0;
			}

			processChar(unicodeChar, hasCtrl, hasFn);
			break;
		}
	}

	public void processChar(int unicodeChar) {
		boolean hasCtrl = ((mSoftMetaState | mInputMetaState) & META_CTRL_ON) != 0;
		boolean hasFn = ((mSoftMetaState | mInputMetaState) & META_FUNCTION_ON) != 0;

		processChar(unicodeChar, hasCtrl, hasFn);
	}

	private void processChar(int unicodeChar, boolean ctrl, boolean fn) {
		int result = unicodeChar;
		if (ctrl) {
			if (unicodeChar >= 'a' && unicodeChar <= 'z') {
				sendByte((byte) (unicodeChar - 'a' + '\001'));
				return;
			} else if (unicodeChar >= 'A' && unicodeChar <= 'Z') {
				sendByte((byte) (unicodeChar - 'A' + '\001'));
				return;
			} else if (unicodeChar == ' ') {
				sendByte(ControlCharacter.NUL);
				return;
			} else if (unicodeChar == '[') {
				sendByte(ControlCharacter.ESC);
				return;
			} else if (unicodeChar == '\\') {
				/* File seperator */
				sendByte((byte) 28);
				return;
			} else if (unicodeChar == ']') {
				/* Group seperator */
				sendByte((byte) 29);
				return;
			} else if (unicodeChar == '^') {
				/* Record seperator */
				sendByte((byte) 30);
				return;
			} else if (unicodeChar == '_') {
				/* Unit seperator */
				sendByte((byte) 31);
				return;
			} else if (unicodeChar == '?') {
				sendByte(ControlCharacter.DEL);
				return;
			}
		}
		if (fn) {
			/* handle special function keys */
		}

		sendByte((byte) result);
	}

	public boolean processKey(int keyCode) {
		
		byte[] bytes;
		if((bytes=KeyBinding.get(keyCode))!=null) {
			sendBytes(bytes);
			return true;
		}
		
		return false;
	}

	private void sendByte(byte b) {
		mResultBuffer[0] = b;
		mListener.onEmulatorInput(mResultBuffer, 1);

		int keys = mKeys.getChildCount();
		for (int i = 0; i < keys; ++i) {
			final View keyView = mKeys.getChildAt(i);
			if (keyView instanceof ToggleButton)
				((ToggleButton) keyView).setChecked(false);
		}
	}

	private void sendBytes(byte[] bytes) {
		mListener.onEmulatorInput(bytes, bytes.length);

		int keys = mKeys.getChildCount();
		for (int i = 0; i < keys; ++i) {
			final View keyView = mKeys.getChildAt(i);
			if (keyView instanceof ToggleButton)
				((ToggleButton) keyView).setChecked(false);
		}
	}

	public void setEmulatorInputListener(EmulatorInputListener listener) {
		mListener = listener;
	}

	public interface EmulatorInputListener {
		public void onEmulatorInput(byte[] b, int length);
	}
}

package org.gscript.terminal;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.util.Log;

/**
 * VT100 escape sequence parser
 * a list of valid VT100 escape sequences can be found at: 
 * {@link http://ascii-table.com/ansi-escape-sequences-vt-100.php}
 */
public class EscapeSequence {
	
	private static final String LOG_TAG = "EscapeSequence";
	private static final int MAX_SEQUENCE_LENGTH = 32;
	
	/* EscapeSequence initiator types ( ESC and intermediate characters ) */
	private static final int ESC = 1;
	private static final int POUND = 2;
	private static final int LPARENTHESIS = 3;
	private static final int RPARENTHESIS = 4;
	private static final int LBRACKET = 5;
	private static final int QUESTIONMARK = 6;
	
	/* Escape + intermediate characters */
	private static final int ESC_POUND = ESC + (10 * POUND);
	private static final int ESC_LPARENTHESIS = ESC + (10 * LPARENTHESIS);
	private static final int ESC_RPARENTHESIS = ESC + (10 * RPARENTHESIS);
	private static final int ESC_LBRACKET = ESC + (10 * LBRACKET);
	private static final int ESC_LBRACKET_QUESTIONMARK = ESC + (10 * LBRACKET) + (100 * QUESTIONMARK);
	
	public static final int SEQUENCE_TYPE_UNKNOWN 		= -1;
	public static final int SEQUENCE_TYPE_UNHANDLED 	= 0;
	/* VT100 */
	public static final int SEQUENCE_TYPE_LMN 			= 1;
	public static final int SEQUENCE_TYPE_DECCKM 		= 2;
	public static final int SEQUENCE_TYPE_DECANM 		= 3;
	public static final int SEQUENCE_TYPE_DECCOLM 		= 4;
	public static final int SEQUENCE_TYPE_DECSCLM 		= 5;
	public static final int SEQUENCE_TYPE_DECSCNM 		= 6;
	public static final int SEQUENCE_TYPE_DECOM 		= 7;
	public static final int SEQUENCE_TYPE_DECAWM		= 8;
	public static final int SEQUENCE_TYPE_DECARM		= 9;
	public static final int SEQUENCE_TYPE_DECINLM		= 10;
	public static final int SEQUENCE_TYPE_DECKPAM		= 11;
	public static final int SEQUENCE_TYPE_DECKPNM		= 12;
	public static final int SEQUENCE_TYPE_SETUKG0		= 13;
	public static final int SEQUENCE_TYPE_SETUKG1		= 14;
	public static final int SEQUENCE_TYPE_SETUSG0		= 15;
	public static final int SEQUENCE_TYPE_SETUSG1		= 16;
	public static final int SEQUENCE_TYPE_SETSPECG0		= 17;
	public static final int SEQUENCE_TYPE_SETSPECG1		= 18;
	public static final int SEQUENCE_TYPE_SETALTG0		= 19;
	public static final int SEQUENCE_TYPE_SETALTG1		= 20;
	public static final int SEQUENCE_TYPE_SETALTSPECG0	= 21;
	public static final int SEQUENCE_TYPE_SETALTSPECG1	= 22;
	public static final int SEQUENCE_TYPE_SS2			= 23;
	public static final int SEQUENCE_TYPE_SS3			= 24;
	public static final int SEQUENCE_TYPE_SGR			= 25;
	public static final int SEQUENCE_TYPE_DECSTBM		= 26;
	public static final int SEQUENCE_TYPE_CUU			= 27;
	public static final int SEQUENCE_TYPE_CUD			= 28;
	public static final int SEQUENCE_TYPE_CUF			= 29;
	public static final int SEQUENCE_TYPE_CUB			= 30;
	public static final int SEQUENCE_TYPE_CNL			= 31;
	public static final int SEQUENCE_TYPE_CPL			= 32;
	public static final int SEQUENCE_TYPE_CHA			= 33;
	public static final int SEQUENCE_TYPE_CUP			= 34;
	public static final int SEQUENCE_TYPE_IND			= 35;
	public static final int SEQUENCE_TYPE_RI			= 36;
	public static final int SEQUENCE_TYPE_NEL			= 37;
	public static final int SEQUENCE_TYPE_DECSC			= 38;
	public static final int SEQUENCE_TYPE_DECRC			= 39;
	public static final int SEQUENCE_TYPE_HTS			= 40;
	public static final int SEQUENCE_TYPE_TBC			= 41;
	public static final int SEQUENCE_TYPE_DECDHL		= 42;
	public static final int SEQUENCE_TYPE_DECSWL		= 43;
	public static final int SEQUENCE_TYPE_DECDWL		= 44;
	public static final int SEQUENCE_TYPE_EL0			= 45;
	public static final int SEQUENCE_TYPE_EL1			= 46;
	public static final int SEQUENCE_TYPE_EL2			= 47;
	public static final int SEQUENCE_TYPE_ED0			= 48;
	public static final int SEQUENCE_TYPE_ED1			= 49;
	public static final int SEQUENCE_TYPE_ED2			= 50;
	public static final int SEQUENCE_TYPE_DSR			= 51;
	public static final int SEQUENCE_TYPE_CPR			= 52;
	public static final int SEQUENCE_TYPE_DA			= 53;
	public static final int SEQUENCE_TYPE_RIS			= 54;
	public static final int SEQUENCE_TYPE_DECALN		= 55;
	public static final int SEQUENCE_TYPE_DECTST		= 56;
	public static final int SEQUENCE_TYPE_DECLL0		= 57;
	public static final int SEQUENCE_TYPE_DECLL1		= 58;
	public static final int SEQUENCE_TYPE_DECLL2		= 59;
	public static final int SEQUENCE_TYPE_DECLL3		= 60;
	public static final int SEQUENCE_TYPE_DECLL4		= 61;
	
	public static final int ARGUMENT_DEFAULT = -1;
	
	private int mInitiatorIndex = 1;
	private int mInitiator = 0;
	private int mType = 0;
	private boolean mFinished;
	
	private int mSequenceLength;
	private ByteBuffer mSequenceBuffer;
	
	private int mArgumentIndex = 0; 
	private ArrayList<Integer> mArguments = new ArrayList<Integer>();
	
	public EscapeSequence(byte b) {
		mSequenceBuffer = ByteBuffer.allocate(MAX_SEQUENCE_LENGTH);
		append(b);
	}
	
	public boolean append(byte b) {
		
		switch(b) {
		/* append reserved characters to the initiator sequence */			
		case ControlCharacter.ESC: 
			appendInitiator(ESC); 
			break;
			
		case ControlCharacter.CSI:
			/* ESC + [ */
			appendInitiator(ESC);
			appendInitiator(LBRACKET);
			break;
			/* intermediate characters */
		case '#': appendInitiator(POUND); 			break;
		case '(': appendInitiator(LPARENTHESIS); 	break;
		case ')': appendInitiator(RPARENTHESIS); 	break;
		case '[': appendInitiator(LBRACKET);		break;
		case '?': appendInitiator(QUESTIONMARK); 	break;

		default:
			switch (mInitiator) {
			case ESC: 
				/* ESC */
				
				switch(b) {
				case ';':
					nextArgument();
					break;
					
				case '=':
					setFinished(SEQUENCE_TYPE_DECKPAM);
					break;
					
				case '>':
					setFinished(SEQUENCE_TYPE_DECKPNM);
					break;
					
				case 'N':
					setFinished(SEQUENCE_TYPE_SS2);
					break;
					
				case 'O':
					setFinished(SEQUENCE_TYPE_SS3);
					break;

				case 'D':
					setFinished(SEQUENCE_TYPE_IND);
					break;
					
				case 'M':
					setFinished(SEQUENCE_TYPE_RI);
					break;
				
				case 'E':
					setFinished(SEQUENCE_TYPE_NEL);
					break;
					
				case '7':
					setFinished(SEQUENCE_TYPE_DECSC);
					break;
					
				case '8':
					setFinished(SEQUENCE_TYPE_DECRC);
					break;

				case 'H':
					setFinished(SEQUENCE_TYPE_HTS);
					break;
					
				case 'c':
					setFinished(SEQUENCE_TYPE_RIS);
					break;
					
				/* VT-52 compatibility only */
					
				case '<':
				case 'F':
				case 'G':
				case 'A':
				case 'B':
				case 'C':
				case 'I':
				case 'K':
				case 'J':
				case 'Z':
					setFinished(SEQUENCE_TYPE_UNHANDLED);
					break;
					
				default:
					if(!updateArgument(b)) {
						Log.e(LOG_TAG, "Unknown byte in sequence: "+ b);
						setFinished(SEQUENCE_TYPE_UNKNOWN);	
					}
					break;
				}
				
				break;
			case ESC_POUND: 
				/* ESC # */
				
				switch(b) {
				case ';':
					nextArgument();
					break;
					
				case '3':
					setFinished(SEQUENCE_TYPE_DECDHL);
					break;
					
				case '4':
					setFinished(SEQUENCE_TYPE_DECDHL);
					break;

				case '5':
					setFinished(SEQUENCE_TYPE_DECSWL);
					break;
					
				case '6':
					setFinished(SEQUENCE_TYPE_DECDWL);
					break;
					
				case '8':
					setFinished(SEQUENCE_TYPE_DECALN);
					break;
					
				default:
					if(!updateArgument(b)) {
						Log.e(LOG_TAG, "Unknown byte in sequence: "+ b);
						setFinished(SEQUENCE_TYPE_UNKNOWN);	
					}
					break;
				}				
				
				break;
			case ESC_LPARENTHESIS: 
				/* ESC ( */
				
				switch(b) {
				case ';':
					nextArgument();
					break;
					
				case 'A':
					setFinished(SEQUENCE_TYPE_SETUKG0);
					break;
					
				case 'B':
					setFinished(SEQUENCE_TYPE_SETUSG0);
					break;
					
				case '0':
					setFinished(SEQUENCE_TYPE_SETSPECG0);
					break;
				
				case '1':
					setFinished(SEQUENCE_TYPE_SETALTG0);
					break;
					
				case '2':
					setFinished(SEQUENCE_TYPE_SETALTSPECG0);
					break;
					
				default:
					if(!updateArgument(b)) {
						Log.e(LOG_TAG, "Unknown byte in sequence: "+ b);
						setFinished(SEQUENCE_TYPE_UNKNOWN);	
					}
					break;
				}				
				
				break;
			case ESC_RPARENTHESIS: 
				/* ESC ) */
				
				switch(b) {
				case ';':
					nextArgument();
					break;
					
				case 'A':
					setFinished(SEQUENCE_TYPE_SETUKG1);
					break;
					
				case 'B':
					setFinished(SEQUENCE_TYPE_SETUSG1);
					break;
					
				case '0':
					setFinished(SEQUENCE_TYPE_SETSPECG1);
					break;
				
				case '1':
					setFinished(SEQUENCE_TYPE_SETALTG1);
					break;
					
				case '2':
					setFinished(SEQUENCE_TYPE_SETALTSPECG1);
					break;
					
				default:
					if(!updateArgument(b)) {
						Log.e(LOG_TAG, "Unknown byte in sequence: "+ b);
						setFinished(SEQUENCE_TYPE_UNKNOWN);	
					}
					break;
				}				
				
				break;
			case ESC_LBRACKET: 
				/* ESC [ */
				
				switch(b) {
				case ';':
					nextArgument();
					break;
					
				case 'h':
					setFinished(SEQUENCE_TYPE_LMN);
					break;
					
				case 'l':
					setFinished(SEQUENCE_TYPE_LMN);
					break;
					
				case 'm':
					setFinished(SEQUENCE_TYPE_SGR);
					break;
					
				case 'r':
					setFinished(SEQUENCE_TYPE_DECSTBM);
					break;
					
				case 'A':
					setFinished(SEQUENCE_TYPE_CUU);
					break;
					
				case 'B':
					setFinished(SEQUENCE_TYPE_CUD);
					break;
					
				case 'C':
					setFinished(SEQUENCE_TYPE_CUF);
					break;
					
				case 'D':
					setFinished(SEQUENCE_TYPE_CUB);
					break;
					
				case 'E':
					setFinished(SEQUENCE_TYPE_CNL);
					break;

				case 'F':
					setFinished(SEQUENCE_TYPE_CPL);
					break;

				case 'G':
					setFinished(SEQUENCE_TYPE_CHA);
					break;
					
				case 'H':
					setFinished(SEQUENCE_TYPE_CUP);
					break;
					
				case 'f':
					/* HVP == CUP */
					setFinished(SEQUENCE_TYPE_CUP);
					break;
					
				case 'g':
					setFinished(SEQUENCE_TYPE_TBC);
					
				case 'K':
					switch(getArgumentOrDefault(0)) {
						case 0: setFinished(SEQUENCE_TYPE_EL0);	break;
						case 1: setFinished(SEQUENCE_TYPE_EL1);	break;
						case 2: setFinished(SEQUENCE_TYPE_EL2);	break;
						default:
							setFinished(SEQUENCE_TYPE_UNKNOWN);
							break;
					}
					break;
					
				case 'J':
					switch(getArgumentOrDefault(0)) {
						case 0: setFinished(SEQUENCE_TYPE_ED0);	break;
						case 1: setFinished(SEQUENCE_TYPE_ED1);	break;
						case 2: setFinished(SEQUENCE_TYPE_ED2);	break;
						default:
							setFinished(SEQUENCE_TYPE_UNKNOWN);
							break;
					}
					break;
					
				case 'n':
					setFinished(SEQUENCE_TYPE_DSR);
					break;
					
				case 'R':
					setFinished(SEQUENCE_TYPE_CPR);
					break;
					
				case 'c':
					setFinished(SEQUENCE_TYPE_DA);
					break;
					
				case 'y':
					setFinished(SEQUENCE_TYPE_DECTST);
					break;
					
				case 'q':
					switch(getArgumentOrDefault(0)) {
						case 0: setFinished(SEQUENCE_TYPE_DECLL0);	break;
						case 1: setFinished(SEQUENCE_TYPE_DECLL1);	break;
						case 2: setFinished(SEQUENCE_TYPE_DECLL2);	break;
						case 3: setFinished(SEQUENCE_TYPE_DECLL3);	break;
						case 4: setFinished(SEQUENCE_TYPE_DECLL4);	break;
						default:
							setFinished(SEQUENCE_TYPE_UNKNOWN);
							break;
					}
					break;

				default:
					if(!updateArgument(b)) {
						Log.e(LOG_TAG, "Unknown byte in sequence: "+ b);
						setFinished(SEQUENCE_TYPE_UNKNOWN);	
					}
					break;
				}					
				
				break;
				
			case ESC_LBRACKET_QUESTIONMARK: 
				/* ESC[? */
				
				switch(b) {
				case ';':
					nextArgument();
					break;
					
				case 'h':
					switch(getArgumentOrDefault(0)) {
						case 1: setFinished(SEQUENCE_TYPE_DECCKM);	break;
						case 2: setFinished(SEQUENCE_TYPE_DECANM);	break;
						case 3: setFinished(SEQUENCE_TYPE_DECCOLM);	break;
						case 4: setFinished(SEQUENCE_TYPE_DECSCLM);	break;
						case 5: setFinished(SEQUENCE_TYPE_DECSCNM);	break;
						case 6: setFinished(SEQUENCE_TYPE_DECOM);	break;
						case 7: setFinished(SEQUENCE_TYPE_DECAWM);	break;
						case 8: setFinished(SEQUENCE_TYPE_DECARM);	break;
						case 9: setFinished(SEQUENCE_TYPE_DECINLM);	break;
						default:
							setFinished(SEQUENCE_TYPE_UNKNOWN);
							break;
					}
					break;
					
				case 'l':
					switch(getArgumentOrDefault(0)) {
						case 1: setFinished(SEQUENCE_TYPE_DECCKM);	break;
						case 2: setFinished(SEQUENCE_TYPE_DECANM);	break;
						case 3: setFinished(SEQUENCE_TYPE_DECCOLM);	break;
						case 4: setFinished(SEQUENCE_TYPE_DECSCLM);	break;
						case 5: setFinished(SEQUENCE_TYPE_DECSCNM);	break;
						case 6: setFinished(SEQUENCE_TYPE_DECOM);	break;
						case 7: setFinished(SEQUENCE_TYPE_DECAWM);	break;
						case 8: setFinished(SEQUENCE_TYPE_DECARM);	break;
						case 9: setFinished(SEQUENCE_TYPE_DECINLM);	break;
						default:
							setFinished(SEQUENCE_TYPE_UNKNOWN);
							break;
					}
					break;

				case 'c':
					setFinished(SEQUENCE_TYPE_DA);
					break;
					
				default:
					if(!updateArgument(b)) {
						Log.e(LOG_TAG, "Unknown byte in sequence: "+ b);
						setFinished(SEQUENCE_TYPE_UNKNOWN);	
					}
					break;
				}				
				
				break;
				
			default:
				
				/* should never happen unless we are receiving
				 * reserved escape characters in a wrong order */
				
				Log.e(LOG_TAG, "Unknown sequence initiator!");
				setFinished(SEQUENCE_TYPE_UNKNOWN);
				break;
			}
		}
		
		if(mSequenceLength < MAX_SEQUENCE_LENGTH) {
			
			/* store the entire escape sequence in a ByteBuffer 
			 * so that we can output it for debugging purposes */
			
			mSequenceBuffer.put(b);
			mSequenceLength++;
			
		} else {

			/* should never happen unless a closing character 
			 * of an escape sequence is missing */
			
			Log.e(LOG_TAG, "Sequence longer then max length.");
			setFinished(SEQUENCE_TYPE_UNKNOWN);
		}
		
		return mFinished;
	}
	
	private boolean updateArgument(byte b) {

		/* check if the value is in range (numeric 0..9) */
		int val = (b - 48);
		if (val >= 0 && val <= 9) {

			/* check if the argument has been created if not do so now and set its value to 0 */
			
			if (mArgumentIndex == mArguments.size())
				mArguments.add(0);

			int arg = mArguments.get(mArgumentIndex);

//			/* set arguments with ARGUMENT_DEFAULT value to 0 */
			if(arg == ARGUMENT_DEFAULT) arg = 0;

			/* argument values come in as a character byte stream, 
			 * every time the argument is updated multiply the existing 
			 * arg value with 10 and then add the new value. */			
			
			arg *= 10;
			arg += val;
			
			/* update argument */
			
			mArguments.set(mArgumentIndex, arg);
			
			return true;
		}
		return false;
	}
	
	private void nextArgument() {
		
		/* arguments are added with ARGUMENT_DEFAULT (-1) as soon as we are actually updating its
		 * value we will first set it to 0. The final EscapeSequence type will decide what value 
		 * an argument with ARGUMENT_DEFAULT will become */
		
		if(mArguments.isEmpty()) {
			
			/* Happens when we are immediatly moving to the next argument without any previous arg value set 
			 * Eg: Esc[;H
			 * Add an argument to compensate so that we will end up with a normalized statement:
			 * Esc[{ARGUMENT_DEFAULT};{ARGUMENT_DEFAULT}H
			 *
			 * */
			
			mArguments.add(ARGUMENT_DEFAULT);
		}
		
		/* raise index and add a new arg value */
		
		mArgumentIndex++;
		mArguments.add(ARGUMENT_DEFAULT);
	}
	
	private void appendInitiator(int type) {
		
		/* multiple initiator type (esc, pound, bracket etc) 
		 * with the index value so that we know the initiator order */
		
		mInitiator += (mInitiatorIndex * type);
		mInitiatorIndex*=10;
	}

	public byte[] getSequence() {
		
		byte[] sequence = new byte[mSequenceLength];
		
		int pos = mSequenceBuffer.position();
		
		mSequenceBuffer.position(0);
		mSequenceBuffer.get(sequence, 0, mSequenceLength);
		mSequenceBuffer.position(pos);
		
		return sequence;
	}
	
	public byte getLastSequenceByte() {
		return (mSequenceLength > 0) ? mSequenceBuffer.get(mSequenceLength-1) : 0;
	}
	
	private void setFinished(int type) {
		mType = type;
		mFinished = true;
	}
	
	public boolean isFinished() {
		return mFinished;
	}
	
	public int getType() {
		return mType;
	}
	
	public ArrayList<Integer> getArguments() {
		return mArguments;
	}

	public Integer getArgumentOrDefault(int index, int defVal) {
		if(mArguments.size() < (index + 1))
			return defVal;
		
		int argVal = mArguments.get(index);
		return (argVal == ARGUMENT_DEFAULT) ? defVal : argVal;
	}
	
	public Integer getArgumentOrDefault(int index) {
		return getArgumentOrDefault(index, 0);
	}
	
	public Integer getArgument(int index) {
		return mArguments.get(index);
	}
	
	public int getArgumentCount() {
		return mArguments.size();
	}
	
	@Override
	public String toString() {

		String strSequence = "";
		byte[] sequence = getSequence();
		for(int i=0; i < sequence.length; ++i) {
			switch(sequence[i]) {
			case ControlCharacter.ESC: strSequence+="ESC"; break;
			case ControlCharacter.CSI: strSequence+="CSI"; break;
			default:
				strSequence+=(char)sequence[i];
			}
		}
		
		Log.d(LOG_TAG, "arguments: ");
		for(int i=0; i < mArguments.size(); ++i)
			Log.d(LOG_TAG, "arg: "+ mArguments.get(i));
		
		return String.format(
				"EscapeSequence ( type:%d, arguments:%d, sequence:%s )", 
				mType, 
				mArguments.size(), 
				strSequence);
	}
}
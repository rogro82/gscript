/*
 * ScreenBuffer.cpp
 *
 *  Created on: Apr 13, 2013
 *      Author: rob
 */

#include <ScreenBuffer.h>
#include <JNIReference.h>
#include <org_gscript_terminal_ScreenBuffer.h>
#include <algorithm>

namespace gscript {

/* ScreenRow */

ScreenRow::ScreenRow() :
		m_flags(ScreenRow::FLAG_LINEWRAP_ENABLED) {
}

ScreenRow::ScreenRow(int flags) :
		m_flags(flags) {
}

/* ScreenBuffer */

ScreenBuffer::ScreenBuffer(int screenRows, int screenCols, int textColor,
		int backColor) :
		m_screenRows(screenRows), m_screenCols(screenCols), m_screenTop(0), m_cursorRow(
				0), m_cursorCol(0), m_scrollTop(0), m_scrollBottom(
				screenRows - 1), m_defTextColor(textColor), m_defBackColor(
				backColor), m_textColor(textColor), m_backColor(backColor), m_textEffects(
				0), m_lineWrap(true) {

	/* insert an empty line to start with */
	m_rows.push_back(ScreenRow());
}

ScreenBuffer::~ScreenBuffer() {
}

void ScreenBuffer::append(char input) {

	if (m_cursorCol == (this->m_screenCols)) {
		lineFeed(ScreenRow::FLAG_LINEWRAPPED);
	}

	int encChar = input;
	encChar = ((m_backColor & 0xff) << 24) | ((m_textColor & 0xff) << 16)
			| ((m_textEffects & 0xff) << 8) | input;

	ScreenRow* row = getRow(m_cursorRow, false);
	if (!row) {
		/* append missing rows */
		for (int i = (m_rows.size()); i <= getCursorRow(true); ++i) {
			m_rows.push_back(ScreenRow(ScreenRow::FLAG_LINEWRAP_ENABLED));
		}
		row = getRow(m_cursorRow, false);
	}

	row->setChar(m_cursorCol, encChar);

	this->m_cursorCol++;
}

void ScreenBuffer::lineFeed(int flags) {

	this->m_cursorRow++;
	this->m_cursorCol = 0;

	if (this->m_cursorRow > (this->m_screenRows - 1)) {
		int offset = m_cursorRow - (this->m_screenRows - 1);

		this->m_screenTop += offset;
		this->m_cursorRow -= offset;
		this->m_cursorCol = 0;
	}

	if (m_lineWrap)
		flags |= ScreenRow::FLAG_LINEWRAP_ENABLED;

	int newrow = getCursorRow(true);
	if ((getRow(newrow, true)) == NULL) {
		/* append missing rows */
		for (int i = (m_rows.size()); i <= newrow; ++i) {
			m_rows.push_back(ScreenRow(flags));
		}
	}
}

void ScreenBuffer::eraseInLine(int opt) {

	ScreenRow* row = getRow(m_cursorRow, false);
	if (row)
		switch (opt) {
		case 1:
			/* Erase from the start of the screen to the active position, inclusive */
			row->clear(0, m_cursorCol + 1);
			break;
		case 2:
			/* Erase all of the line, inclusive */
			row->clear();
			break;
		default:
			/* Erase from the active position to the end of the line, inclusive (default) */
			row->limit(m_cursorCol);
			break;
		}
}

void ScreenBuffer::eraseInScreen(int opt) {

	ScreenRow* row = NULL;

	switch (opt) {
	case 1:
		/* Erase from start of the screen to the active position, inclusive */
		for (int i = 0; i < m_cursorRow; ++i) {
			if ((row = getRow(i, false)))
				row->clear();
		}
		row->clear(0, ++m_cursorCol);
		break;
	case 2:
		/* Erase all of the display – all lines are erased, changed to single-width, and the cursor does not move. */
		for (int i = 0; i < m_screenRows; ++i) {
			if ((row = getRow(i, false)))
				row->clear();
		}
		break;
	default:
		/* Erase from the active position to the end of the screen, inclusive (default) */
		if ((row = getRow(m_cursorRow, false)))
			row->limit(m_cursorCol);

		for (int i = (m_cursorRow + 1); i < m_screenRows; ++i) {
			if ((row = getRow(i, false)))
				row->clear();
		}
		break;
	}
}

void ScreenBuffer::setGraphicsRendition(int param) {

	switch (param) {
	case 0:

		/* reset all styles */
		m_textEffects = TextEffects::DEFAULT;
		m_textColor = m_defTextColor;
		m_backColor = m_defBackColor;

		break;
		/* set styles */
	case 1:
		m_textEffects |= TextEffects::BOLD;
		break;
	case 3:
		m_textEffects |= TextEffects::ITALIC;
		break;
	case 4:
		m_textEffects |= TextEffects::UNDERLINE;
		break;
	case 5:
	case 6:
		m_textEffects |= TextEffects::BLINK;
		break;
	case 7:
		m_textEffects |= TextEffects::INVERSE;
		break;
	case 8:
		m_textEffects |= TextEffects::INVISIBLE;
		break;
		/* unset style */
	case 21:
		m_textEffects &= ~TextEffects::BOLD;
		break;
	case 23:
		m_textEffects &= ~TextEffects::ITALIC;
		break;
	case 24:
		m_textEffects &= ~TextEffects::UNDERLINE;
		break;
	case 25:
	case 26:
		m_textEffects &= ~TextEffects::BLINK;
		break;
	case 27:
		m_textEffects &= ~TextEffects::INVERSE;
		break;
	case 28:
		m_textEffects &= ~TextEffects::INVISIBLE;
		break;

	default:

		if (param >= 30 && param <= 37) {
			/* set textcolor 30-37 */
			m_textColor = (param - 30);

		} else if (param >= 40 && param <= 47) {
			/* set backcolor 40-47 */
			m_backColor = (param - 40);

		} else {
			LOGD("%s unhandled %d", __FUNCTION__, param);
		}
	}
}

void ScreenBuffer::resize(int rows, int cols) {

	ScreenRow* row = getRow(m_cursorRow, false);
	if (row) {
		row->setFlag(ScreenRow::FLAG_CURSOR_SAVED);
	}

	m_screenRows = rows;

	if (cols != m_screenCols) {

		/* re-wrap all rows */

		m_screenCols = cols;

		ScreenRows wrappedRows;
		ScreenRows resizedRows;

		for (ScreenRows::reverse_iterator reverse_row_iterator =
				m_rows.rbegin(); reverse_row_iterator != m_rows.rend();
				reverse_row_iterator++) {

			if (reverse_row_iterator->hasFlag(ScreenRow::FLAG_LINEWRAPPED)) {
				wrappedRows.push_back(*reverse_row_iterator);
			}

			else {

				ScreenRow row = *reverse_row_iterator;

				if (!wrappedRows.empty()) {
					for (ScreenRows::reverse_iterator reverse_wrapped_iterator =
							wrappedRows.rbegin();
							reverse_wrapped_iterator != wrappedRows.rend();
							reverse_wrapped_iterator++) {

						row.appendRow(*reverse_wrapped_iterator);
					}
					wrappedRows.clear();
				}

				int charCount = row.getCharCount();
				if (row.hasFlag(ScreenRow::FLAG_LINEWRAP_ENABLED)
						&& (charCount > m_screenCols)) {

					ScreenRows rowWrap;
					row.wrap(rowWrap, m_screenCols);

					/* resizedRows is in reverse order so reverse rowWrap */
					std::reverse(rowWrap.begin(), rowWrap.end());

					resizedRows.insert(resizedRows.end(), rowWrap.begin(),
							rowWrap.end());

				} else {
					resizedRows.push_back(row);
				}
			}
		}

		std::reverse(resizedRows.begin(), resizedRows.end());
		this->m_rows.swap(resizedRows);
	}

	if (m_cursorRow > (m_screenRows - 1)) {
		m_cursorRow = (m_screenRows - 1);
	}
	if (m_cursorRow < 0) {
		m_cursorRow = 0;
	}

	int cursorOffset = 0;

	for (ScreenRows::reverse_iterator reverse_row_iterator = m_rows.rbegin();
			reverse_row_iterator != m_rows.rend(); reverse_row_iterator++) {

		if (reverse_row_iterator->hasFlag(ScreenRow::FLAG_CURSOR_SAVED)) {
			m_screenTop = (m_rows.size() - 1) - (cursorOffset + m_cursorRow);
			reverse_row_iterator->clearFlag(ScreenRow::FLAG_CURSOR_SAVED);
			break;
		}
		cursorOffset++;
	}

	/* remove left-over rows that are out of scope ( > screen top + screen rows ) */

	int minRows = std::min((size_t)(m_screenTop + m_screenRows), m_rows.size());
	m_rows.resize(minRows, ScreenRow(ScreenRow::FLAG_LINEWRAP_ENABLED));
}

void ScreenBuffer::setCursorRow(int index) {
	this->m_cursorRow = index;
}

void ScreenBuffer::setCursorCol(int index) {
	this->m_cursorCol = index;
}

void ScreenBuffer::setCursor(int row, int col) {
	setCursorRow(row);
	setCursorCol(col);
}

void ScreenBuffer::moveCursor(int rowdir, int coldir) {
	setCursorRow(m_cursorRow + rowdir);
	setCursorCol(m_cursorCol + coldir);
}

const int ScreenBuffer::getTextColor() const {
	return this->m_defTextColor;
}

const int ScreenBuffer::getBackColor() const {
	return this->m_defBackColor;
}

const int ScreenBuffer::getScreenRows() const {
	return this->m_screenRows;
}

const int ScreenBuffer::getScreenCols() const {
	return this->m_screenCols;
}

const int ScreenBuffer::getScreenTop() const {
	return this->m_screenTop;
}

const int ScreenBuffer::getScreenFillTop() const {

	const ScreenRow* row = NULL;

	int fillTop = m_screenTop;
	if(fillTop < m_screenRows)
		return fillTop;

	for(int i=0; i < m_screenRows; ++i) {
		int index = (m_screenRows-1) - i;

		if((row=getRow(index, false))!=NULL) {
			fillTop = fillTop - i;
			break;
		}
	}

	return fillTop;
}

const int ScreenBuffer::getCursorRow(bool absolute) const {
	return (absolute) ?
			this->m_screenTop + this->m_cursorRow : this->m_cursorRow;
}

const int ScreenBuffer::getCursorCol() const {
	return this->m_cursorCol;
}

ScreenRows::iterator ScreenBuffer::getRow(int row, bool absolute) {

	int absrow = row;

	if (!absolute)
		absrow += this->m_screenTop;

	if (absrow >= (int) this->m_rows.size()) {
		return NULL;
	}

	ScreenRows::iterator iter = ScreenRows::iterator(m_rows.begin());

	return iter += absrow;
}

ScreenRows::const_iterator ScreenBuffer::getRow(int row, bool absolute) const {

	int absrow = row;

	if (!absolute)
		absrow += this->m_screenTop;

	if (absrow >= (int) this->m_rows.size()) {
		return NULL;
	}

	ScreenRows::const_iterator iter = ScreenRows::const_iterator(
			m_rows.begin());

	return iter += absrow;
}

} /* namespace gscript */

JNIEXPORT jlong JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeCreate(
		JNIEnv *env, jclass clazz, jint screenRows, jint screenCols,
		jint foreColor, jint backColor) {

	ref_ptr<gscript::ScreenBuffer> sb = new gscript::ScreenBuffer(screenRows,
			screenCols, foreColor, backColor);
	JNIReference* ref = new JNIReference(sb.get());

	return (jlong) ref;
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeAppend(
		JNIEnv *env, jclass clazz, jlong jptr, jbyte input) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->append(input);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeLineFeed(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->lineFeed();
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeEraseInLine(
		JNIEnv *env, jclass clazz, jlong jptr, jint opt) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->eraseInLine(opt);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeEraseInScreen(
		JNIEnv *env, jclass clazz, jlong jptr, jint opt) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->eraseInScreen(opt);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeSetGraphicsRendition(
		JNIEnv *env, jclass clazz, jlong jptr, jintArray jparams) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	int paramLength = env->GetArrayLength(jparams);

	jint *params = env->GetIntArrayElements(jparams, 0);
	for (int i = 0; i < paramLength; ++i) {
		sb->setGraphicsRendition(*params);
		params++;
	}
	env->ReleaseIntArrayElements(jparams, params, 0);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeResize(
		JNIEnv *env, jclass clazz, jlong jptr, jint rows, jint cols) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->resize(rows, cols);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeSetCursorRow(
		JNIEnv *env, jclass clazz, jlong jptr, jint index) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->setCursorRow(index);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeSetCursorCol(
		JNIEnv *env, jclass clazz, jlong jptr, jint index) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->setCursorCol(index);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeSetCursor(
		JNIEnv *env, jclass clazz, jlong jptr, jint row, jint col) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->setCursor(row, col);
}

JNIEXPORT void JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeMoveCursor(
		JNIEnv *env, jclass clazz, jlong jptr, jint rowdir, jint coldir) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	sb->moveCursor(rowdir, coldir);
}

JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetTextColor(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getTextColor();
}

JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetBackColor(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getBackColor();
}

JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetScreenRows(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getScreenRows();
}

JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetScreenCols(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getScreenCols();
}

JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetScreenTop(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getScreenTop();
}

JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetScreenFillTop
  (JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getScreenFillTop();
}


JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetCursorRow(
		JNIEnv *env, jclass clazz, jlong jptr, jboolean absolute) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getCursorRow(absolute);
}

JNIEXPORT jint JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetCursorCol(
		JNIEnv *env, jclass clazz, jlong jptr) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);
	return sb->getCursorCol();
}

JNIEXPORT jboolean JNICALL Java_org_gscript_terminal_ScreenBuffer_nativeGetRowData(
		JNIEnv *env, jclass clazz, jlong jptr, jint row, jboolean absolute,
		jobject buffer) {

	ref_ptr<gscript::ScreenBuffer> sb = JNIReference::cast<
			gscript::ScreenBuffer*>(jptr);

	int screenCols = sb->getScreenCols();
	int capacity = env->GetDirectBufferCapacity(buffer);

	if (capacity != (4 + (4 * screenCols)))
		return false;

	gscript::ScreenRows::const_iterator screenRow = sb->getRow(row, absolute);
	int* bufferPtr = (int*) env->GetDirectBufferAddress(buffer);

	if (screenRow == NULL) {

		*bufferPtr = -1;
		bufferPtr++;
		for (int i = 0; i < screenCols; ++i) {
			*bufferPtr = gscript::EMPTY_COLUMN;
			bufferPtr++;
		}

	} else {
		/* set flag */
		*bufferPtr = screenRow->getFlags();
		bufferPtr++;

		/* copy characters */
		std::vector<int>::const_iterator itr = screenRow->getChars();

		int charCount = screenRow->getCharCount();

		/* make sure the row does not contain more characters
		 * then the buffer we copy to */

		charCount = (charCount > screenCols) ? screenCols : charCount;

		for (int i = 0; i < charCount; ++i, itr++, bufferPtr++) {
			*bufferPtr = *itr;
		}

		if (screenCols - charCount > 0) {
			/* add empty characters if needed */
			for (int i = 0; i < (screenCols - charCount); ++i, bufferPtr++) {
				*bufferPtr = gscript::EMPTY_COLUMN;
			}
		}
	}

	return true;
}

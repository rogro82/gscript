/*
 * ScreenBuffer.h
 *
 *  Created on: Apr 13, 2013
 *      Author: rob
 */

#ifndef SCREENBUFFER_H_
#define SCREENBUFFER_H_

#include <Object.h>
#include <vector>
#include <iterator>

namespace gscript {

struct TextEffects {

	static const int DEFAULT =		0;
	static const int BOLD = 		0x1;
	static const int ITALIC = 		0x2;
	static const int UNDERLINE = 	0x4;
	static const int BLINK = 		0x8;
	static const int INVERSE = 		0x10;
	static const int INVISIBLE = 	0x20;
	static const int IGNORE =		0x40;
};

static const int EMPTY_COLUMN =	(((TextEffects::IGNORE) << 8) | ' ');

class ScreenRow {
public:

	static const int FLAG_DEFAULT = 0x0;
	static const int FLAG_LINEWRAP_ENABLED = 0x1;
	static const int FLAG_LINEWRAPPED = 0x2;
	static const int FLAG_CURSOR_SAVED = 0x8000;

	ScreenRow();
	ScreenRow(int flags);

	const int getFlags() const {
		return m_flags;
	}

	const bool hasFlag(int flag) const {
		return (m_flags & flag) == flag;
	}

	void setFlag(int flag) {
		m_flags |= flag;
	}

	void clearFlag(int flag) {
		m_flags &= ~flag;
	}

	const int getCharCount() const {
		return this->m_chars.size();
	}

	void appendChar(int c) {
		this->m_chars.push_back(c);
	}

	void appendRow(const ScreenRow& row) {
		m_chars.insert(m_chars.end(), row.m_chars.begin(), row.m_chars.end());
	}

	void setChar(size_t index, int c) {

		if(this->m_chars.size()==index) {
			appendChar(c);
		} else if(this->m_chars.size() < index) {
			int emptyChars = (index - m_chars.size());
			for(int i=0; i < emptyChars; ++i)
				appendChar(EMPTY_COLUMN);

			appendChar(c);

		} else if(this->m_chars.size() > index) {
			m_chars.at(index)=c;
		}
	}

	void clear() {
		m_chars.clear();
	}

	void clear(size_t start, size_t length) {

		if(start > m_chars.size())
			return;

		if(start == 0 && length >= m_chars.size())
			m_chars.clear();

		if((start != 0 && (start+length) >= m_chars.size()))
			limit(start);

		for(size_t i=0; i < length; ++i) {
			setChar((start+i), EMPTY_COLUMN);
		}
	}

	void limit(size_t max) {
		if(m_chars.size() > max)
			m_chars.resize(max, EMPTY_COLUMN);
	}

	void wrap(std::vector<ScreenRow>& splitRows, size_t size) {

		int splitCount = (m_chars.size() + (size-1)) / size;
		splitRows.reserve(splitCount);
		for(int idx=0; idx < splitCount; ++idx) {

			int flags = this->m_flags;
			if(idx != 0)
				flags |= FLAG_LINEWRAPPED;

			ScreenRow splitRow(flags);

			int offset = idx * size;
			int end = std::min((idx+1) * size, m_chars.size());

			splitRow.m_chars.insert(splitRow.m_chars.begin(), m_chars.begin() + offset, m_chars.begin() + end);
			splitRows.push_back(splitRow);
		}
	}

	const int getCharAt(size_t index) const {
		return *(m_chars.begin()+index);
	}

	std::vector<int>::const_iterator getChars() const {
		return std::vector<int>::const_iterator(m_chars.begin());
	}

private:
	int m_flags;
	std::vector<int> m_chars;

};

typedef std::vector<ScreenRow> ScreenRows;

class ScreenBuffer :
	public Referenceable {
public:

	ScreenBuffer(int screenRows, int screenCols, int textColor, int backColor);

	void append(char input);

	void lineFeed(int flags=0);
	void eraseInLine(int opt=0);
	void eraseInScreen(int opt=0);

	void setGraphicsRendition(int param);
	void resize(int rows, int cols);

	void setCursorRow(int index);
	void setCursorCol(int index);
	void setCursor(int row, int col);
	void moveCursor(int rowdir, int coldir);

	const int getTextColor() const;
	const int getBackColor() const;

	const int getScreenRows() const;
	const int getScreenCols() const;
	const int getScreenTop() const;
	const int getScreenFillTop() const;

	const int getCursorRow(bool absolute=false) const;
	const int getCursorCol() const;

	ScreenRows::iterator getRow(int row, bool absolute=false);
	ScreenRows::const_iterator getRow(int row, bool absolute=false) const;

protected:
	virtual ~ScreenBuffer();

private:

	int m_screenRows;
	int m_screenCols;
	int m_screenTop;

	int m_cursorRow;
	int m_cursorCol;

	int m_scrollTop;
	int m_scrollBottom;

	int m_defTextColor;
	int m_defBackColor;

	int m_textColor;
	int m_backColor;
	int m_textEffects;

	bool m_lineWrap;

	ScreenRows m_rows;
};

} /* namespace gscript */
#endif /* SCREENBUFFER_H_ */

/*
 * Object.h
 *
 *  Created on: Jan 7, 2013
 *      Author: rob
 */

#ifndef OBJECT_H_
#define OBJECT_H_

#include <boost/ref_ptr.h>

namespace gscript {

template<bool Referenceable = true>
class Object: public RefCountable {
protected:
	Object() {
	}
	virtual ~Object() {
	}
};

template<>
class Object<false> {
protected:
	Object() {
	}
	virtual ~Object() {
	}
};

typedef Object<>	Referenceable;

}

#endif /* BASEOBJECT_H_ */

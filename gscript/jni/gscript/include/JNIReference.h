/*
 * JNIReference.h
 *
 *  Created on: Jan 7, 2013
 *      Author: rob
 */

#ifndef JNIREFERENCE_H_
#define JNIREFERENCE_H_

#include <Object.h>
#include <jni.h>

typedef jlong jpointer;

namespace gscript {

class JNIReference {
public:

	JNIReference(Referenceable* object);

	Referenceable* get();

	/*
	 * fast implementation to unwrap from a JNI jlong to a given derived class of Referenceable
	 * */
	template<class T>
	static inline T cast(jpointer ptr) {
		return static_cast<T>(static_cast<JNIReference*>((void*) ptr)->get());
	}

	static void dispose(jpointer ptr);

private:

	virtual ~JNIReference();
	ref_ptr<Referenceable> m_refobj;
};

}

/* also expose outside of namespace using typedef for convenience */
typedef gscript::JNIReference JNIReference;

#endif /* JNIREFERENCE_H_ */

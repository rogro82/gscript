/*
 * JNIReference.cpp
 *
 *  Created on: Jan 7, 2013
 *      Author: rob
 */

#include <JNIReference.h>
#include <org_gscript_jni_JNIReference.h>

namespace gscript {

JNIReference::JNIReference(Referenceable *object) {
	this->m_refobj = object;
}

JNIReference::~JNIReference() {

	this->m_refobj = NULL;
}

Referenceable* JNIReference::get() {
	return this->m_refobj.get();
}

void JNIReference::dispose(jpointer ptr) {

	JNIReference* ref = static_cast<JNIReference*>((void*) ptr);
	if (ref)
		delete (ref);
}

}

JNIEXPORT void JNICALL Java_org_gscript_jni_JNIReference_nativeDispose(
		JNIEnv *env, jclass clazz, jpointer ptr) {

	JNIReference::dispose(ptr);
}

JNIEXPORT jlong JNICALL Java_org_gscript_jni_JNIReference_nativeCloneReference(
		JNIEnv *env, jclass clazz, jlong src) {

	/* create a new JNIReference which points to the same Referenceable as src */

	JNIReference* clonedRef = new JNIReference(
			JNIReference::cast<gscript::Referenceable*>(src));

	return (jlong) clonedRef;
}

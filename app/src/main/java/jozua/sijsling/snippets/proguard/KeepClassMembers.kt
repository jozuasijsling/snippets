package jozua.sijsling.snippets.proguard

/** Annotation to stop proguard / R8 from obfuscating class members, see 'annotations.pro'  */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class KeepClassMembers

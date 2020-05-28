package jozua.sijsling.snippets.proguard

/** Annotation to stop proguard / R8 from renaming class members, see 'annotations.pro'  */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class KeepClassMemberNames

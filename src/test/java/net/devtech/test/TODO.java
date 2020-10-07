package net.devtech.test;

public class TODO {
	// todo for curseforge, make a mod called "Iridis API Index" or include it in "Iridis Loader", and make it just have an index of version -> curseforge links
	// so when a mod requires X api, it automatically installs all the bridge versions and stuff

	// todo deal with array casting problem
	// todo atleast make warning for array methods n stuff
	// todo realize that delegates don't work because interfaces exist

	// todo deal with overriding method signatures, so if Class<A> exists <A> void foo() is still valid
	// todo allow manual abstraction
	// todo not support inner instance classes
	// todo don't abstract annon classes by accident :concern:

	// todo for interface abstractions that have stdlib super classes, add a asSuper method or something
	// todo for base abstractions that have stdlib super classes indirectly, make the super class the stdlib class, because it's still valid via indirect inheritance

	// todo extension methods, they delegate to a static method that you specify in a config
	// todo that way can gen sources at the same time as abstraction, it also handles part of ladder for us
	// todo and we can add javadocs cus it's in a config
	/*@Static for static methods, instance should be excluded*/
	public static Object /*return type*/ myExtension(Object instance, Object... params) {
		return null;
	}

	// todo output api bytecode and source, bytecode is to fool intellij and source is for viewing, it'll have stub methods, compileOnly in grDle
	// todo output impl bytecode and source, bytecode is what is present at runtime, and source is for Iridis contributers, runtimeOnly

	// todo annotation processor for implementing api interfaces
	// todo make the api jar contain the annotation processor

	// todo jar processor for live viewing of implementation
	// todo use mojmap to fill in names
	// todo layered mappings for docs, and merger for yarn PRs
	// todo code visitor abstraction

	// todo registry sync, mod resources
	// todo loader api, for cross platform shid
	// todo we'd like have like 3 subprojects, vanilla, forge and fabric
	// todo and the abstracter configs are capable of inheriting from one another, and modifying each other
	// todo and remapping, that way we can add compat for all platforms fairly easily ish:tm:
}

### For Curseforge
make a mod called "Iridis API Index" or include it in "Iridis Loader", and make it just have an index of version -> curseforge links
so when a mod requires X api, it automatically installs all the bridge versions and stuff
### Updating
so, if a class abstraction can't be saved, let's say there's a conflict in return types or something, or a single
method is impossible to adjust, then copy the **old** class, with the original api version, delegate
any methods, and then increment the api version for the class in abstracter.

The end result should be, 1 class in the v1 package, who delegates it's calls to the v2 class.
At some point, if we have like 100 v1 classes, then we'd just seperate it out into a seperate jar.


If a method needs updating, but is still backwards compatible, then just use mixin.

### Deal With Array Casting Problem
atleast make warning for array methods n stuff

### for interface abstractions
 that have stdlib super classes, add a asSuper method or something
### extension methods
they delegate to a static method that you specify in a config

### javadocs
for extension methods too
```java
public class MyClass {
    /*@Static for static methods, instance should be excluded*/
    public static Object /*return type*/ myExtension(Object instance, Object... params) {
        return null;
    }
}
```


### annotation processor for implementing api interfaces
or just blame everything on eclipse and vscode users for having a bad ide that doesn't support good annotations\
make the api jar contain the annotation processor
### use mojmap to fill in names

# platforms
todo registry sync, mod resources\
todo loader api, for cross platform shid\
todo we'd like have like 3 subprojects, vanilla, forge and fabric\
todo and remapping, that way we can add compat for all platforms fairly easily ish:tm:
### For Curseforge
make a mod called "Iridis API Index" or include it in "Iridis Loader", and make it just have an index of version -> curseforge links
so when a mod requires X api, it automatically installs all the bridge versions and stuff
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
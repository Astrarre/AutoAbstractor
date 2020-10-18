todo for curseforge, make a mod called "Iridis API Index" or include it in "Iridis Loader", and make it just have an index of version -> curseforge links
so when a mod requires X api, it automatically installs all the bridge versions and stuff
todo deal with array casting problem
todo atleast make warning for array methods n stuff
todo allow manual abstraction
todo for interface abstractions that have stdlib super classes, add a asSuper method or something
todo extension methods, they delegate to a static method that you specify in a config
todo javadocs, for extension methods too

```java
public class MyClass {
    /*@Static for static methods, instance should be excluded*/
    public static Object /*return type*/ myExtension(Object instance, Object... params) {
        return null;
    }
}
```


todo annotation processor for implementing api interfaces
todo make the api jar contain the annotation processor
todo use mojmap to fill in names

todo registry sync, mod resources
todo loader api, for cross platform shid
todo we'd like have like 3 subprojects, vanilla, forge and fabric
todo and remapping, that way we can add compat for all platforms fairly easily ish:tm:
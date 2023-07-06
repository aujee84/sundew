This annotation processor creates property file and helper AutoConfig class which is used to initialize fields annotated with @AutoConfig on startup.
@AutoConfig handles static fields in class type with String and every primitive or wrapper type.

Build process is bound to Maven compile lifecycle.

1. Be sure that you are running Maven with jdk >= 19 from Maven startup.
2. Add filter if you have profiles (optional).
```` 
        <filters>
            <filter>src/main/resources/env/application.${env}.properties</filter>
        </filters>
        <resources>
            <resource>
                <directory>target/generated-sources/annotations/resources</directory>
                <filtering>true</filtering>
                <includes>
                    <include>*.properties</include>
                </includes>
            </resource>
        </resources>
````
3. Install lib to your local maven repository.
4. Annotate your uninitialized static fields.
5. Compile - processor will create raw application.properties file in generated-sources directory, and AutoConfig helper class. Example:
````
   org.company.com.mypackage.MyClass.keyStoreFile=${org.company.com.mypackage.MyClass.keyStoreFile}
````
6. Copy application.properties to standard location : src/main/resources/env and write property values.
7. If filter and profiles are used change file name (e.g. application.dev.properties).
8. If you really don't want to show your properties create a local profile e.g.
````
<profile>
            <id>local</id>
            <properties>
                <env>local</env>
            </properties>
            <build>
                <plugins>
                    <plugin>
                        <groupId>org.codehaus.mojo</groupId>
                        <artifactId>properties-maven-plugin</artifactId>
                        <version>${properties-maven-plugin}</version>
                        <executions>
                            <execution>
                                <phase>initialize</phase>
                                <goals>
                                    <goal>read-project-properties</goal>
                                </goals>
                                <configuration>
                                    <urls>
                                        <url>file:///${user.home}/path/${module.name}.properties</url>
                                    </urls>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </build>

        </profile>
        <profile>
            <id>dev</id>
            <properties>
                <env>dev</env>
            </properties>
        </profile>
````
Now application.local.properties can be empty.

9. Compile - maven will map values and provide proper application.properties file to standard output.
10. To initialize in runtime use:
````
AutoInitializer.ConfigurationHandler.initialize();
````
11. Whenever you want to use installed annotation processor be sure that -J--enable-preview is present in pom's compiler plugin configuration arg in the module that uses the annotation processor.
12. To build/run in IDE you have two options: 
    a) Delegate IDE build and run to Maven or
    b) for Intellij
    run/debug configurations: -> add VM options :--enable-preview        
    settings -> build, execution, deployment -> compiler-> Shared build process VM options : --enable-preview
        
    
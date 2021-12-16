# DemoChat

A simple chat plugin that features different channels.

## Channels

By default, 4 channels are included in `config.yml`.

* `global`: Messages get sent to anyone, anywhere, in any world, on the server.
* `shout`: Messages get sent to anyone in the same world as you.
* `local`: Messages get sent to anyone within a 20 block radius.
* `whisper`: Messages get sent to anyone within a 3 block radius.

### Development

DemoChat features minimal public methods and events, of which you can access using the maven repository.

Insert the following snippets into your POM.xml.

For the repository:

```xml

<repositories>
    ...
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/regulad/DemoChat</url>
    </repository>
    ...
</repositories>
```

For the dependency:

```xml

<dependencies>
    ...
    <dependency>
        <groupId>xyz.regulad</groupId>
        <artifactId>demochat</artifactId>
        <version>{version}</version>
        <scope>provided</scope>
    </dependency>
    ...
</dependencies>
```

Replace `{version}` with the current version. You can see the current version below. Don't include the "v".

![Current Version](https://img.shields.io/github/v/release/regulad/DemoChat)



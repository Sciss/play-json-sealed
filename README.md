# play-json-sealed

[![Flattr this](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=sciss&url=https%3A%2F%2Fgithub.com%2FSciss%2Fplay-json-sealed&title=play-json-sealed&language=Scala&tags=github&category=software)
[![Build Status](https://travis-ci.org/Sciss/play-json-sealed.svg?branch=master)](https://travis-ci.org/Sciss/play-json-sealed)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/play-json-sealed_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/play-json-sealed_2.11)

## statement

This project is meant as an extension for the `Json.format` macro in the JSON library of the [play framework](https://github.com/mandubian/play-json-alone) which  generates automatic JSON serializers. It provides a new macro `AutoFormat` which supports sealed trait types and falls back to the original macro for other types.

It is released under the [GNU Lesser General Public License](https://raw.github.com/Sciss/play-json-sealed/master/LICENSE) v2.1+ and comes with absolutely no warranties. It might be submitted to Play at some point. To contact the author, send an email to `contact at sciss.de`.

## requirements / installation

This project currently compiles against Scala 2.11 and 2.10 using sbt 0.13.

To use the library in your project:

    "de.sciss" %% "play-json-sealed" % v

The current version `v` is `"0.4.0"`, corresponding with play-json version 2.3.10, the
last to support Java 6 and 7. We are currently not interested in supporting
Play 2.4.6 because of its incompatibility with Java 6/7.

The following resolver must be added to find the `play-json` dependency:

    "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/"

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## example

Examples are provided by way of test sources (`sbt test`). Basically you get a format via `AutoFormat[A]`. If that trait requires formats for other traits, you need to put their respective formats implicitly in scope, first. There are also a few useful formats in `Formats`:

- `FileFormat` for `java.io.File`
- `Tuple2Format` for any `Tuple2`, given that its type parameters have implicit formats
- `VecFormat` for a `collection.immutable.IndexedSeq`, given that its element type has an implicit format
- `OptionFormat` for any `Option` - this was stupidly removed from Play-JSON in 2.4.x.

## limitations and issues

- Recursive types are not yet possible (see `RecursiveTest`).
- There is a scoping issue. If you have `object Foo { case class Bar(i: Int) }`, then you need to __import__ the `Bar` symbol for the macros to work. E.g. `Json.format[Foo.Bar]` fails, whereas `import Foo.Bar; Json.format[Bar]` works.

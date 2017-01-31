Firmware Fragmenter
===================

This application splits a firmware update package into several fragments. It is
used to update the firmware of ConnectCore modules through
[Device Cloud](https://devicecloud.digi.com/login.do).

Usage
-----

Execute the following command:

```
java -jar FirmwareFragmenter-<x.y.z>.jar [-s <fragment_size_mb>] [-d <device_directory>] <update_package.zip>
```

Where:

 * `<x.y.z>` is the version of the Firmware Fragmenter tool.
 * `<fragment_size_mb>` is the fragment size in MB. This parameter is optional,
the default value is 40 MB.
 * `<device_directory>` is the directory in the ConnectCore file system where
 you have to put the generated fragments. This parameter is optional, the
 default value is `/storage/emulated/legacy`.
 * `<update_package.zip>` is the path to the update package ZIP file to split.

The fragments are generated inside the `out` directory.

Build
-----

Use [Apache Maven](https://maven.apache.org/) to build and package the Firmware
Fragmenter tool by issuing the following command from the Firmware Fragmenter
directory, where the `pom.xml` file is located:

```
mvn package
```

To cleanly build and generate the JAR artifact use the following call:

```
mvn clean package
```

The Java code is built and packaged in a JAR file inside the `target` directory.
The name of the output JAR file is `FirmwareFragmenter-x.y.z.jar`, where `x.y.z`
is the version of the tool, e.g.: `FirmwareFragmenter-1.0.0.jar`.

License
-------

Copyright (c) 2016, 2017, Digi International Inc.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

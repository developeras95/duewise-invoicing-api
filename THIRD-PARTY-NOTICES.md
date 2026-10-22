# Third-party notices

Duewise itself is proprietary (see [LICENSE](LICENSE)). It is built on open-source
libraries that keep their own licences. Those licences apply to those libraries
only, and Duewise's licence does not restrict anything they allow.

## Full list

[THIRD-PARTY.txt](THIRD-PARTY.txt) lists every library shipped in the application jar,
with its version and licence. It is generated from the Maven build, so it always
matches `pom.xml`. Regenerate it whenever a dependency is added or upgraded:

```
./mvnw license:add-third-party
```

Test-only libraries (JUnit, Mockito, Testcontainers, GreenMail and similar) are not
shipped and are not listed.

## OpenHTMLtoPDF (LGPL)

Duewise renders PDF invoices with
[OpenHTMLtoPDF](https://github.com/openhtmltopdf/openhtmltopdf)
(`io.github.openhtmltopdf:openhtmltopdf-core` and `openhtmltopdf-pdfbox`, version 1.1.87),
which is licensed under the
[GNU Lesser General Public License, version 2.1 or later](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html).

- Duewise uses the library unmodified, through its public API.
- In the executable jar the library is kept as separate jar files under
  `BOOT-INF/lib/`, so a recipient can replace them with another (including a
  modified) version of OpenHTMLtoPDF.
- The library's source code is available from its repository linked above.

## Libraries offered under a choice of licences

When `THIRD-PARTY.txt` shows several licences on one line, the library is offered
under any one of them, at the user's choice. Duewise uses each of these under the
non-GPL option:

| Library | Offered under | Duewise uses |
| --- | --- | --- |
| Jakarta Mail API, Angus Mail | EDL 1.0, EPL 2.0, or GPL 2.0 with Classpath Exception | EDL 1.0 (BSD-3-Clause) |
| Jakarta Annotations API, Jakarta Transaction API | EPL 2.0, or GPL 2.0 with Classpath Exception | EPL 2.0 |
| Logback Classic, Logback Core | EPL 2.0, or LGPL 2.1 | EPL 2.0 |

## Eclipse Public License 2.0 components

AspectJ Weaver, Jakarta Persistence API, Jakarta Annotations API, Jakarta Transaction
API and Logback are used unmodified under the
[Eclipse Public License 2.0](https://www.eclipse.org/legal/epl-2.0/). Their source code
is available from the project pages listed against each of them in `THIRD-PARTY.txt`.

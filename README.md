[![Clojars Project](https://img.shields.io/clojars/v/io.github.fourteatoo/klanar.svg?include_prereleases)](https://clojars.org/io.github.fourteatoo/klanar)
[![cljdoc badge](https://cljdoc.org/badge/io.github.fourteatoo/klanar)](https://cljdoc.org/d/io.github.fourteatoo/klanar)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/fourteatoo/klanar/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/fourteatoo/klanar/tree/main)
[![Coverage Status](https://coveralls.io/repos/github/fourteatoo/klanar/badge.svg)](https://coveralls.io/github/fourteatoo/klanar)

# klanar

Renew expiring ads on KleinAnzeige.de (the German online second-hand
market place).

It browse your email looking for expiration warnings.  Those are
usually sent a week before the ad expires.  It identifies the
extension link and clicks for you.

## Installation

Compile

    $ lein uberjar

then copy the jar file somewhere you can find again

    $ cp target/uberjar/klanar-<version>-standalone.jar some/where/else

## Usage

    $ java -jar target/uberjar/klanar-<version>-standalone.jar -h
	
will get you an usage string.  It should be straightforward.

You need a configuration file before you can use the application.  The
standard configuration file is `~/.klanar`, but you can specify
another one withg the `-c` command line option.

    $ java -jar target/klanar-<version>-standalone.jar
	
it fetches you latest email and checks if there is any new message
from kleinanzige.de, and processes them.  

Sample output:

```
~/Projects/klanar $ java -jar target/uberjar/klanar-0.1.0-SNAPSHOT-standalone.jar
INFO [2026-01-13 18:10:47,454] main - fourteatoo.klanar.core process messages 
INFO [2026-01-13 18:10:50,483] main - fourteatoo.klanar.core processing 1 messages
INFO [2026-01-13 18:10:50,488] main - fourteatoo.klanar.core ad “sports shirt” is expiring (https://www.kleinanzeigen.de/s-anzeige/lego-set/28365763296-22-65234?utm_source=email&utm_medium=system_email&utm_campaign=email-ExtendAd&utm_content=VIP)
```

## Options

  -c, --config FILE     configuration file
  -n, --dry-run         don't actually renew the ads
  -m, --monitor         enter monitor mode
  -d, --days N          fetch last N days
  -l  --list-folders    list all folders on the server
  -v, --verbose         increase logging verbosity
  -h, --help            display program usage


The `-d` option overrides the `[:search :days]` configuration
parameter.

## Configuration

The default configuration file is `~/.klanar`.  It should look
something like this:

```clojure
;; -*- Clojure -*-
{:imap {:user "you@domain.com"
        :password "verysecret"
        :session {"mail.imaps.host" "imap.your-provider.com"
                  "mail.imaps.port" 993}}
 :after-processing {:mark-as-seen true
                    :move-to "kleinanzeigen"}
 :search {:from "noreply@kleinanzeigen.de"
          :to "you@domain.com"
          :days 14}}
```


### Bugs

Expected.


## License

Copyright © 2026 Walter C. Pelissero

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.





















FIXME: description

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar klanar-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2026 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.

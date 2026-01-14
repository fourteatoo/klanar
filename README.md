[![CircleCI](https://dl.circleci.com/status-badge/img/gh/fourteatoo/klanar/tree/main.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/fourteatoo/klanar/tree/main)
[![Coverage Status](https://coveralls.io/repos/github/fourteatoo/klanar/badge.svg)](https://coveralls.io/github/fourteatoo/klanar)

# klanar

klanar is an application that renews expiring ads on KleinAnzeige.de
(the German online classified ads board; former eBay).

It browses (or monitors) your email, looking for expiration warnings.
Those are usually sent a week before the ad expires.  It identifies
the renewal link and clicks it for you.

## Installation

Compile

    $ lein uberjar

then copy the jar file somewhere you can find again

    $ cp target/uberjar/klanar-<version>-standalone.jar your/bin/directory/klanar.jar
	
then copy the shell script, which you can tailor to your needs:

    $ cp klanar.sh your/bin/directory/klanar
	
By default the trampoline script runs the jar file of the same name,
but with the `.jar` extension.

## Usage

You need a configuration file before you can use the program.  The
standard configuration file is `~/.klanar`, but you can specify
another one withg the `-c` command line option.

Try

    $ java -jar klanar-<version>-standalone.jar -h
	
or, if you have installed the trampoline script:

    $ klanar -h

You will be provided with a list of acceptable options.  It should be
self-explanatory.

Without command line options

    $ klanar
	
browses you latest email and checks if there is any new message from
kleinanzige.de.  Any such message is processed (triggering the
renewal) and set aside.  That is, depending on your configuration, it
is marked as "seen" nad/or moved to a different mail folder.

Sample output:

```
~/Projects/klanar $ java -jar target/uberjar/klanar-0.1.0-SNAPSHOT-standalone.jar
INFO [2026-01-13 18:10:47,454] main - fourteatoo.klanar.core process messages 
INFO [2026-01-13 18:10:50,483] main - fourteatoo.klanar.core processing 1 messages
INFO [2026-01-13 18:10:50,488] main - fourteatoo.klanar.core ad “sports shirt” is expiring (https://www.kleinanzeigen.de/s-anzeige/lego-set/28365763296-22-65234?utm_source=email&utm_medium=system_email&utm_campaign=email-ExtendAd&utm_content=VIP)
```

### Monitor mode

klanar can run in monitor mode, if the options `-m` is passed on the
command line.  In monitor mode, klanar waits for changes in your
mailbox and if new messages come in, they are parsed and processed as
necessary.

    $ klanar -m


## Options

```
  -c, --config FILE     configuration file
  -n, --dry-run         don't actually renew the ads
  -m, --monitor         enter monitor mode
  -d, --days N          fetch last N days
  -l  --list-folders    list all folders on the server
  -v, --verbose         increase logging verbosity
  -h, --help            display program usage
```

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

The default mailbox is INBOX.  If you want to change that, you can add
the `:folder` in the `:imap` map. Example:

```clojure
 :imap {:user "your-account@gmail.com"
        :password "*****"
        :session {"mail.imaps.host" "imap.gmail.com"
                  "mail.imaps.port" 993}
        :folder "mymailbox"}
```

The `:after-processing` part specifies what to do with the messages
(only the relevant ones) after klanar is done with them.  By default
klanar will mark them as "seen" and leave them where it found them.
If, for instance, you prefer to drop them "unopened" in a differend
folder, say, "renews", you can place following in you configuration:

```clojure
 :after-processing {:mark-as-seen false
                    :move-to "renews"}
```

If the folder doesn't exist, it is automatically created for you.

klanar doesn't touch the messages that don't match the search
parameters.  It's up to you to make sure that those are correct and
sufficient.  Search parameters can specify the sender (`:from`), the
receiver (`:to`), the period (`:days`), the subject (`:subject`) and
even messages that have been already seen (`:unread false`).  The
choice is up to you.

```clojure
 :search {:from "noreply@kleinanzeigen.de"
          :to "you@domain.com"
          :days 14}
```

If the default logging on the console doesn't suit you, you can add
something like:

```clojure
 :logging {:level :info
           :console false
           :appenders [{:appender :rolling-file
                        :rolling-policy {:type :fixed-window
                                         :max-index 5}
                        :triggering-policy {:type :size-based
                                            ;; 5MB
                                            :max-size 5242880}                        
                        :file "/home/you/klanar.log"
                        :encoder :pattern}]
           :overrides {"fourteatoo.klanar.imap" :debug}}
```

Have a look at https://github.com/pyr/unilog for more information.


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

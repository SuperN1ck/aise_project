# aise_project

## How to run
Either execute `gradlew` on Unix/MacOS or `gradlew.bat` on Windows.
To install `evaluation` do:

`./gradlew assemble shadowJar`
`python3 evaluation/runEvoSQL.py erpnext evosql`

## How to use JDB (Java Debugger)?

manually I made `evosql/evaluation/run.sh` for java debugging.

check the port number (ex. 12000), and run `sh run.sh`.

then, open another terminal and go to the same directory (~/evosql)

type 'jdb -attach SAME_PORT(ex. 12000)'


basic command is,

`step` : step into the present line
`next` : implement next line
`stop at [ClassName]:[line]` : set breakpoint
`cont` : continue until meeting breakpoint

you can study more to google it.

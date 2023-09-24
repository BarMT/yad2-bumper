Script to keep bumping up your ads in `yad2.co.il` automatically.

The JVM version is the simplest (and recommended) way to run the script, but requires Java installed.
The Native version is standalone but is (slightly) more pain to compile.

## JVM
`scala-cli compile Yad2.scala`
`scala-cli run Yad2.scala --  <email> <pass>`

## Native
needed libs:
`dnf install libcurl-devel libidn2-devel`

build with:
`scala-cli --power package --native Yad2-native.scala -o yad2 -f`

run with:
`./yad2 <email> <pass>`

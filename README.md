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
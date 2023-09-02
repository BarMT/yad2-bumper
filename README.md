needed libs:
`dnf install libcurl-devel libidn2-devel`

build with:
`scala-cli --power package --native Yad2.scala -o yad2 -f`

run with:
`./yad2 <email> <pass>`
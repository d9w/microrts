
default:
	javac -cp src:lib/jdom.jar:lib/minimal-json-0.9.4.jar src/tests/StateEvalDataGen.java

run:
	java -cp src:lib/jdom.jar:lib/minimal-json-0.9.4.jar tests.StateEvalDataGen

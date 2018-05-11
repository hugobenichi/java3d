OUTDIR=out
CLASSPATH=out/:jars/lwjgl.jar:jars/lwjgl_util.jar
MAIN="Game"
SOURCE="src/*.java"

.DEFAULT_GOAL := build

builddir:
	mkdir -p $(OUTDIR)

$(OUTDIR)/Game.class: src/*.java
	javac -d $(OUTDIR) -cp $(CLASSPATH) $<

build: builddir $(OUTDIR)/Game.class

run: build
	java -Djava.library.path=natives/ -cp $(CLASSPATH) $(MAIN)

clean:
	rm -rf $(OUTDIR)

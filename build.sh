#!/bin/bash
# Build script for L-System Explorer
# Requires Java 17+

mkdir -p build/classes

javac -d build/classes \
  src/model/LSystem.java \
  src/ui/Theme.java \
  src/ui/DrawPanel.java \
  src/ui/GrammarPanel.java \
  src/ui/MainFrame.java \
  src/main/LSystemExplorer.java

if [ $? -eq 0 ]; then
  echo "Compilation successful."
  echo "Main-Class: main.LSystemExplorer" > build/MANIFEST.MF
  jar cfm LSystems.jar build/MANIFEST.MF -C build/classes .
  echo "JAR created: LSystems.jar"
  echo "Run with:    java -jar LSystems.jar"
else
  echo "Compilation failed."
fi

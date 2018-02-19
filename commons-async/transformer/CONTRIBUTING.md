## Transformation

Stackless continuations

fsm wih switch and jumps

handling constructors calls with await() params (would end up with NEW separate from <init> call leading 
to invalid class, need to move the NEW to after the await() call)

handling synchronized blocks

## Debugging

If a problem occurs, run mvn with -X switch to see stack trace (for non-forked compilation).

To debug, run mvnDebug and attach (for non-forked compilation) or update the forked JVM command 
line to be able to attach a debugger.

https://github.com/Storyyeller/Krakatau is a useful disassembler with easy to read output (Requires python 2). 

Intellij ASM plugin is also useful to see the bytecode outline or ASM version of the transformed class.
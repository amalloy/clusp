Clusp is a Clojure interpreter for [SNUSP](http://esolangs.org/wiki/SNUSP), an extension of Brainfuck to two dimensions. Conceptually, the program's thread of execution has a position and a direction: it executes the Brainfuck instruction on the program "map" at its current position, and then moves in its current direction. However, there are special SNUSP instructions for moving control flow, rather than the one-dimensional Brainfuck `[` and `]` for looping: `/` and `\` are "mirrors" that reflect the program's direction by ninety degrees, and `?` is a conditional, causing the next instruction to be skipped if the current memory cell is zero. Because of the mirrors, loops in the program's thread of execution are represented by actual loops drawn in the program's two-dimensional code.

To use, call the `snusp` function with:
- A string describing the program grid: each line is a row, and each character an instruction.
- A sequence of numbers to give the program as input. You can hard-code this as an actual list, or use a lazy sequence that, for example, prompts the user for input. To truly conform to the SNUSP standard, these numbers should represent ASCII characters, but you can cheat by inputting actual numbers if you write your program to expect this.

`snusp` will run the program, and return a vector of all values output by the program before its termination.

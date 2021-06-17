# The Hinton Language

![Hinton Logo](Assets/Logos/Logo-wide.png)

This is a stack-based, multi-pass, bytecode interpreter written in Rust for a programming language called Hinton. The project is an extension of the code found in the book [Crafting Interpreters](https://craftinginterpreters.com/) by Bob Nystrom.

## Features
Though this interpreter is based on the Crafting Interpreters book, it implements many things differently. Here are some of the differences between Hinton and the language developed in the book (Lox):

* Hinton source code is first parsed into an Abstract Syntax Tree (AST), then compiled to bytecode, then interpreted by the VM. This is because traversing the AST allows for easier bytecode generation and optimization (optimization strategies will be added later).

* Hinton has extra built-in data structures like `Arrays`, `Tuples`, `Iterators`, `Ranges`, and `Dictionaries`.

* Hinton has extra built-in functions like:
    * `print(...)`: To print to the console,
    * `input(...)`: To receive user input,
    * `iter(...)`: To convert an object to an iterator,
    * `next(...)`: To get the next item in an iterator,
    * `assert(...)`: To test that an expression is truthy,
    * `assert_eq(...)`: To test that two expressions are equal, and
    * `assert_ne(...)`: To test that two expressions are not equal,

* Hinton has support for more operators like `%`, `**`, `<<`, `>>`, `^`, `&`, `~`, nullish coalescing (`??`), ternary conditionals (`? :`), advanced reassignment (`+=`, `**=`, `%=`, etc...), plus binary, hexadecimal, and octal numbers.

* Hinton supports the `break` and `continue` statements in loops.

* Hinton supports the "long" version of almost all instructions that have an argument. For example, while the `DEFINE_GLOBAL` instruction takes the next byte as its operand (only allowing 255 global variables to be declared), the `DEFINE_GLOBAL_LONG` instruction takes the next two bytes as its operand (allowing up to 65,536 global variables to be declared).

** Hinton is a work-in-progress, and many other features are yet to come. To see a list of the features currently being worked on, visit the [Planned Features](https://github.com/hinton-lang/Hinton/projects/1) page. For a list of features without a near-by implementation date, visit the [Missing Features](#missing-features) section of this README.

<sub>**NOTE:** All highlighted Hinton code in this README is being highlighted by GitHub's Swift syntax highlighter for illustration purposes only. The code is not actual Swift code, and GitHub does not provide a syntax Highlight for Hinton code.</sub>

## Hello World
To run a "hello world" program, simply download and unzip this repo, create a file named `hello-world.ht` on your computer, and place the following inside it:
```swift
print("Hello Hinton!");
```

Then, in a terminal window, navigate to the unzipped folder and run:
```
cargo run '</path/to/hello-world.ht>'
```
NOTE: You must install Rust to run Hinton. I know, I know, but Hinton isn't a full programming language yet, so this will have to do.

## Advanced Programs
### The Classic Fibonacci Number Calculator:
On average, running with release mode, the algorithm takes ~105ms to compute the `fib(25)` on my MacBook Pro 2019 with 16GB of RAM running MacOS Big Sur. For comparison, a similar program in Python takes ~24ms. Not very fast, but not super slow either.
```swift
func fib(n := 0) {
    if (n < 2) return n;
    return fib(n - 2) + fib(n - 1);
}

let the_25th_fib = fib(25);

print(the_25th_fib);
```
### A Little Greeting Loop
```swift
while true {
    let name = input("Who are we greeting? ");
    print("Hello there, " + name + "!\n");

    if input("Greet again? (y/n): ") != "y" {
        break;
    }
}
```

## Lifecycle of a Hinton Program
Hinton programs get executed in three separate steps: parsing, compiling and executing.
* **Parsing**: The parser finds tokens in the source code and groups those tokens into `ASTNode`s to create an Abstract Syntax Tree (AST) of the source code. This syntax tree can be analyzed for code optimizations like Constant Folding and Loop Unrolling (optimizations coming in the future).

* **Compiling**: The compiler takes an AST, walks the tree, and generates bytecode instructions as it goes. It creates a `SymbolTable` to keep track of declarations made in local scopes and enforces lexical scoping at compile time so that the VM does not have to perform checks for the existence of variables at runtime. (You can also [print the bytecode](#printing-bytecode) of a program).

* **Executing**: The execution step involves the creation of a stack-based Virtual Machine (VM). The VM takes a chunk of bytecode and executes one instruction in the chunk at a time. It works by pushing and popping objects onto an Object stack where it stores local variables and temporary objects. It also has a Frames stack, where it pushes and pops function call frames.

Because Hinton programs are executed in these three separate steps, they takes longer to start execution. To see the time each step takes to execute, run the programs with the `bench_time` Cargo feature flag:
```
cargo run --features bench_time </path/to/program.ht>
```
This should print a message similar to the following after the program finishes executing:
```
======= ⚠️  Execution Results ⚠️  =======
Parse Time:     <parsing time>
Compile Time:   <compile time>
Run Time:       <execution time>
=======================================
```

## Printing Bytecode
To print the generated bytecode for a program, run the file with the `show_bytecode` Cargo feature flag:
```
cargo run --features show_bytecode </path/to/program.ht>
```
For example, running the following program from a file called `./test.ht` results in the following bytecode:

**Program**
```swift
let x = 0;

while x <= 10 {
    print("X equals " + x);
    x += 1;
}
```
**Bytecode**
```
==== <File '/path/to/file.ht'> ====
00001   00000 0x11 – LOAD_IMM_0I                
  |     00001 0x25 – DEFINE_GLOBAL              0 -> 'x'
00003   00003 0x27 – GET_GLOBAL                 0 -> 'x'
  |     00005 0x2C – LOAD_IMM_N                 10
  |     00007 0x0F – LESS_THAN_EQ               
  |     00008 0x47 – POP_JUMP_IF_FALSE          30 (add 19 to IP)
00004   00011 0x2D – LOAD_NATIVE                7 -> 'print'
  |     00013 0x2B – LOAD_CONSTANT              1 -> (X equals )
  |     00015 0x27 – GET_GLOBAL                 0 -> 'x'
  |     00017 0x00 – ADD                        
  |     00018 0x26 – FUNC_CALL                  1
00003   00020 0x20 – POP_STACK_TOP              
00005   00021 0x27 – GET_GLOBAL                 0 -> 'x'
  |     00023 0x13 – LOAD_IMM_1I                
  |     00024 0x00 – ADD                        
  |     00025 0x33 – SET_GLOBAL                 0 -> 'x'
00004   00027 0x20 – POP_STACK_TOP              
00003   00028 0x2E – LOOP_JUMP                  3 (sub 27 from IP)
00000   00030 0x08 – END_VIRTUAL_MACHINE
```

And to see the raw bytes, run the file with the `show_raw_bytecode` flag:
```
cargo run --features show_raw_bytecode </path/to/program.ht>
```
Which, for the above program, results in the following chunk of bytes:
```
==== <File '/path/to/file.ht'> ====
0x11 0x25 0x00 0x27 0x00 0x2C 0x0A 0x0F 
0x47 0x00 0x13 0x2D 0x07 0x2B 0x01 0x27 
0x00 0x00 0x26 0x01 0x20 0x27 0x00 0x13 
0x00 0x33 0x00 0x20 0x2E 0x1B 0x08 

Chunk Size: 31
================
```

## Missing Features
I initially started reading the Crafting Interpreters book with no knowledge of compilers, interpreters, ASTs, or bytecode. I also did not know how to write Rust programs until February of 2021 (and I still have a lot to learn about it). Because of this, translating the code found in the last chapters of the book has been quite difficult. Even with those challenges, I am still trying to add as many smaller features as possible while also trying to improve the three components of the interpreter before moving on. Here is a list of features that Hinton is currently missing and that may take longer to be added:
* Lambda expressions.
* Garbage Collection
* Classes & Inheritance
* Importing Modules
* Native methods bound to primitive objects (i.e., `Array.len()`)

## Contributing
Because I am creating Hinton to learn about compiler/interpreter design and programming language implementation, I will not be accepting any pull requests that add any of the above *missing features* to Hinton (I want to learn how to do it myself). However, any other contributions that improve the current state of the interpreter are welcomed. For a list of planned features or issues to which you can contribute visit the [Planned Features](https://github.com/hinton-lang/Hinton/projects/1) or [Issues](https://github.com/hinton-lang/Hinton/issues) page.

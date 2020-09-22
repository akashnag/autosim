AutoSim
=====================

AutoSim is a free automata simulator for students and educators. Written in Java, it is a command-line based utility that runs on any OS that supports Java. With AutoSim, you can not only determine the final state or stack/tape contents, you can also trace through the execution states as well, from the initial to the final states. It currently supports DFAs, NFAs, Moore machines, Mealy machines, DPDAs, NPDAs, CFGs, as well as Standard Turing Machines.

### Compilation

```bash
$ cd src/autosim
$ javac -Xlint:unchecked *.java
```

### Execution

```bash
$ cd src
$ java autosim.AutoSim
```

The above will display the list of all command line options. Example files are to be found in the `examples` directory.

### License

The application is licensed under the MIT License. Copyright &copy; Akash Nag.
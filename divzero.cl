
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    main():Object {{
        io.out_int(2/0);
    }};
};


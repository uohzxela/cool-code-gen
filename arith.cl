
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    main():Object {{
        io.out_int(1+2+(3+4+5+(6+(7+8+(9+10)))));
        io.out_string("\n");
        io.out_int(28/2);
        io.out_string("\n");
        io.out_int(2 * 7);
        io.out_string("\n");
        io.out_int(15-1);
        io.out_string("\n");
    }};
};


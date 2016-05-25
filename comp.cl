
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    main():Object {{
        if 5 = 6 then
            io.out_string("true")
        else
            io.out_string("false")
        fi;
    }};
};


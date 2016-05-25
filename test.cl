(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    add(a:Int, b:Int):Int {a+b};
    main():Object {
        --io.out_int(123456789)
        add(1,2)
    };
};


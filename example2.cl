
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class A {};
class B inherits A {};
class Main {
    io:IO;
    a:Int;
    b:A;
    c:String;
    d:Bool;
    main():Object {{
        io <- new IO;
        io.out_int(123); 
    }};
};


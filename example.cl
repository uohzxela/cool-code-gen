
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class A {};
class B inherits A {};
class Main {
    io:IO <- new IO;
    a:Int <- 11;
    b:A;
    c:String <- "asdfasdfasdfasdfasdf";
    d:Bool <- true;
    main():Object {{
        io <- new IO;
        io.out_int(123); 
    }};
};


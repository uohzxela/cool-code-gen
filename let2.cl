
(*  Example cool program testing as many aspects of the code generator
    as possible.
 *)
class Main {
    io:IO <- new IO;
    --e:Int <- let a:Int <- 100 in a;
    main():Object {{
        --let x:Int <- 1, y:Int <- x+10, z:Int<- 13, w:IO, a:Int, b:Int, c:Int, d:Int in io.out_int(e);
        let x:Int <- 1 in
            let x:Int <- 10 in
                io.out_int(x);
    }};
};


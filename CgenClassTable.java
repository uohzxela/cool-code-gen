/*
   Copyright (c) 2000 The Regents of the University of California.
   All rights reserved.

   Permission to use, copy, modify, and distribute this software for any
   purpose, without fee, and without written agreement is hereby granted,
   provided that the above copyright notice and the following two
   paragraphs appear in all copies of this software.

   IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
   DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
   OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
   CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

   THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
   INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
   AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
   ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
   PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */

// This is a project skeleton file

import java.io.PrintStream;
import java.util.Vector;
import java.util.Enumeration;
import java.util.*;

/** This class is used for representing the inheritance tree during code
  generation. You will need to fill in some of its methods and
  potentially extend it in other useful ways. */
class CgenClassTable extends SymbolTable {
    final static int AR_SIZE = 3;

    /** All classes in the program, represented as CgenNode */
    private Vector nds;
    private Vector reorderedNds = new Vector();

    /** This is the stream to which assembly instructions are output */
    private PrintStream str;

    private int stringclasstag;
    private int intclasstag;
    private int boolclasstag;
    // DEPRECATED
    private int basicclasstag;
    
    public int tempFPOffset = 0;
    public int labelCount = 0;
    // DEPRECATED
    private int classtag = 0;

    private HashMap<AbstractSymbol, ArrayList<attr>> classAttrs;
    private HashMap<AbstractSymbol, ArrayList<Method>> classMethods;
    private HashMap<AbstractSymbol, Integer> classTags;
    
    public int getLabel() { 
        return labelCount++;
    }
    public int getClassTag(AbstractSymbol clsName) {
        return classTags.get(clsName);
    }
    public ArrayList<attr> getAttrs(AbstractSymbol clsName) {
        return classAttrs.get(clsName);
    }
    public void enterTempScope() {
        enterScope();
        tempFPOffset--;
    }
    public void exitTempScope() {
        exitScope();
        tempFPOffset++;
    }
    public Location getLocation(AbstractSymbol so, AbstractSymbol id) {
        ArrayList<attr> attrs = classAttrs.get(so);
        for (int i=0; i<attrs.size(); i++) {
            attr a = attrs.get(i);
            if (a.getName() == id) {
                return new Location(CgenSupport.SELF, i + AR_SIZE);
            }
        }
        return null;
    }

    public Location getLocation(AbstractSymbol so, AbstractSymbol methodName, AbstractSymbol id) {
        Object objInLetScope = lookup(id);
        if (objInLetScope != null) return (Location) objInLetScope;
        if (methodName == null) return getLocation(so, id);
        ArrayList<Method> methods = classMethods.get(so);
        for (Method method : methods) {
            if (method.name == methodName) {
                Formals fs = method.m.formals;
                int formalsLength = fs.getLength();
                for (int i=0; i<formalsLength; i++) {
                    formal f = (formal)fs.getNth(i);
                    if (f.getName() == id) {
                        return new Location(CgenSupport.FP, formalsLength - 1 - i + AR_SIZE);
                    }
                }
            }
        }
        return getLocation(so, id);
    }
    public void emitStaticDispatch(AbstractSymbol so, AbstractSymbol methodName) {
        CgenSupport.emitLoadAddress(CgenSupport.T1, so + CgenSupport.DISPTAB_SUFFIX, str);
        ArrayList<Method> methods = classMethods.get(so);
        for (int i=0; i < methods.size(); i++) {
            Method method = methods.get(i);
            if (method.name == methodName) {
                CgenSupport.emitLoad(CgenSupport.T1, i, CgenSupport.T1, str);
                CgenSupport.emitJalr(CgenSupport.T1, str);
            }
        }
    }

    public void emitDispatch(AbstractSymbol so, AbstractSymbol methodName) {
        CgenSupport.emitLoad(CgenSupport.T1, CgenSupport.DISPTABLE_OFFSET, CgenSupport.ACC, str);
        ArrayList<Method> methods = classMethods.get(so);
        for (int i=0; i < methods.size(); i++) {
            Method method = methods.get(i);
            if (method.name == methodName) {
                CgenSupport.emitLoad(CgenSupport.T1, i, CgenSupport.T1, str);
                CgenSupport.emitJalr(CgenSupport.T1, str);
            }
        }
    }
    // The following methods emit code for constants and global
    // declarations.

    /** Emits code to start the .data segment and to
     * declare the global names.
     * */
    private void codeGlobalData() {
        // The following global names must be defined first.

        str.print("\t.data\n" + CgenSupport.ALIGN);
        str.println(CgenSupport.GLOBAL + CgenSupport.CLASSNAMETAB);
        str.print(CgenSupport.GLOBAL); 
        CgenSupport.emitProtObjRef(TreeConstants.Main, str);
        str.println("");
        str.print(CgenSupport.GLOBAL); 
        CgenSupport.emitProtObjRef(TreeConstants.Int, str);
        str.println("");
        str.print(CgenSupport.GLOBAL); 
        CgenSupport.emitProtObjRef(TreeConstants.Str, str);
        str.println("");
        str.print(CgenSupport.GLOBAL); 
        BoolConst.falsebool.codeRef(str);
        str.println("");
        str.print(CgenSupport.GLOBAL); 
        BoolConst.truebool.codeRef(str);
        str.println("");
        str.println(CgenSupport.GLOBAL + CgenSupport.INTTAG);
        str.println(CgenSupport.GLOBAL + CgenSupport.BOOLTAG);
        str.println(CgenSupport.GLOBAL + CgenSupport.STRINGTAG);

        // We also need to know the tag of the Int, String, and Bool classes
        // during code generation.

        str.println(CgenSupport.INTTAG + CgenSupport.LABEL 
                + CgenSupport.WORD + intclasstag);
        str.println(CgenSupport.BOOLTAG + CgenSupport.LABEL 
                + CgenSupport.WORD + boolclasstag);
        str.println(CgenSupport.STRINGTAG + CgenSupport.LABEL 
                + CgenSupport.WORD + stringclasstag);

    }

    /** Emits code to start the .text segment and to
     * declare the global names.
     * */
    private void codeGlobalText() {
        str.println(CgenSupport.GLOBAL + CgenSupport.HEAP_START);
        str.print(CgenSupport.HEAP_START + CgenSupport.LABEL);
        str.println(CgenSupport.WORD + 0);
        str.println("\t.text");
        str.print(CgenSupport.GLOBAL);
        CgenSupport.emitInitRef(TreeConstants.Main, str);
        str.println("");
        str.print(CgenSupport.GLOBAL);
        CgenSupport.emitInitRef(TreeConstants.Int, str);
        str.println("");
        str.print(CgenSupport.GLOBAL);
        CgenSupport.emitInitRef(TreeConstants.Str, str);
        str.println("");
        str.print(CgenSupport.GLOBAL);
        CgenSupport.emitInitRef(TreeConstants.Bool, str);
        str.println("");
        str.print(CgenSupport.GLOBAL);
        CgenSupport.emitMethodRef(TreeConstants.Main, TreeConstants.main_meth, str);
        str.println("");
    }

    /** Emits code definitions for boolean constants. */
    private void codeBools(int classtag) {
        BoolConst.falsebool.codeDef(classtag, str);
        BoolConst.truebool.codeDef(classtag, str);
    }

    /** Generates GC choice constants (pointers to GC functions) */
    private void codeSelectGc() {
        str.println(CgenSupport.GLOBAL + "_MemMgr_INITIALIZER");
        str.println("_MemMgr_INITIALIZER:");
        str.println(CgenSupport.WORD 
                + CgenSupport.gcInitNames[Flags.cgen_Memmgr]);

        str.println(CgenSupport.GLOBAL + "_MemMgr_COLLECTOR");
        str.println("_MemMgr_COLLECTOR:");
        str.println(CgenSupport.WORD 
                + CgenSupport.gcCollectNames[Flags.cgen_Memmgr]);

        str.println(CgenSupport.GLOBAL + "_MemMgr_TEST");
        str.println("_MemMgr_TEST:");
        str.println(CgenSupport.WORD 
                + ((Flags.cgen_Memmgr_Test == Flags.GC_TEST) ? "1" : "0"));
    }

    /** Emits code to reserve space for and initialize all of the
     * constants.  Class names should have been added to the string
     * table (in the supplied code, is is done during the construction
     * of the inheritance graph), and code for emitting string constants
     * as a side effect adds the string's length to the integer table.
     * The constants are emmitted by running through the stringtable and
     * inttable and producing code for each entry. */
    private void codeConstants() {
        // Add constants that are required by the code generator.
        AbstractTable.stringtable.addString("");
        AbstractTable.inttable.addString("0");

        AbstractTable.stringtable.codeStringTable(stringclasstag, str);
        AbstractTable.inttable.codeStringTable(intclasstag, str);
        codeBools(boolclasstag);
    }
    
    private void codeClassNameTab() {
        str.print(CgenSupport.CLASSNAMETAB); str.print(CgenSupport.LABEL); //label
        //Vector nds = reorderedNds;
        for (int i=0; i<nds.size(); i++) {
            CgenNode cls = (CgenNode)nds.get(i);
            str.print(CgenSupport.WORD); 
            StringSymbol clsName = (StringSymbol)AbstractTable.stringtable.lookup(cls.name.toString());
            clsName.codeRef(str); str.println();// .word str_const<num>
        }
    }

    private void codeClassObjTab() {
        str.print(CgenSupport.CLASSOBJTAB); str.print(CgenSupport.LABEL); //label
        //Vector nds = reorderedNds;
        for (int i=0; i<nds.size(); i++) {
            CgenNode cls = (CgenNode)nds.get(i);
            str.print(CgenSupport.WORD); 
            CgenSupport.emitProtObjRef(cls.name, str); str.println(); // .word <Class>_protObj
            str.print(CgenSupport.WORD); 
            CgenSupport.emitInitRef(cls.name, str); str.println(); // .word <Class>_init
        }
    }

    private void codeClassDispTab() {
        for (int i=0; i<nds.size(); i++) {
            CgenNode cls = (CgenNode)nds.get(i);
            str.print(cls.name + CgenSupport.DISPTAB_SUFFIX); str.print(CgenSupport.LABEL);
            codeClassMethods(cls);       
        }
    }
    private void buildClassAttrs() {
        CgenNode root = (CgenNode) lookup(TreeConstants.Object_);
        buildClassAttrs(root, new ArrayList<attr>());
    }
    private void buildClassAttrs(CgenNode cls, ArrayList<attr> attrs) {
        if (cls == null) return;
        Features features = cls.features;  
        for (int i=0; i<features.getLength(); i++) {
            Feature f = (Feature)features.getNth(i);
            if (f instanceof method) {
                continue;
            }
            attr a = (attr) f;
            attrs.add(a);
        }
        classAttrs.put(cls.name, attrs);
        for (Enumeration e = cls.getChildren(); e.hasMoreElements(); ) {
            buildClassAttrs((CgenNode)e.nextElement(), new ArrayList<attr>(attrs) );
        }
        
    }

    private void buildClassMethods() {
        CgenNode root = (CgenNode) lookup(TreeConstants.Object_);
        buildClassMethods(root, new ArrayList<Method>());
    }
    private void buildClassMethods(CgenNode cls, ArrayList<Method> methods) {
        if (cls == null) return;
        Features features = cls.features;  
        for (int i=0; i<features.getLength(); i++) {
            Feature f = (Feature)features.getNth(i);
            if (f instanceof attr) {
                continue;
            }
            method m = (method) f;
            boolean shouldAdd = true;
            for (Method method : methods) {
                if (method.name == m.name) {
                    method.cls = cls.name;
                    method.m = m;
                    shouldAdd = false;
                    break;
                }
            }
            if (shouldAdd) {
                methods.add(new Method(m.name, cls.name, m));
            }
            
        }
        classMethods.put(cls.name, methods);
        for (Enumeration e = cls.getChildren(); e.hasMoreElements(); ) {
            buildClassMethods((CgenNode)e.nextElement(), cloneMethods(methods) );
        }
        
    }

    private ArrayList<Method> cloneMethods(ArrayList<Method> methods) {
        ArrayList<Method> newMethods = new ArrayList<Method>();
        for (Method m : methods) {
            newMethods.add(new Method(m.name, m.cls, m.m));
        }
        return newMethods;
    }
    private void codeClassMethods(CgenNode cls) {
        for (Method m : classMethods.get(cls.name)) {
            str.print(CgenSupport.WORD);
            str.println(m.cls + CgenSupport.METHOD_SEP + m.name);
        }
    }
    private void codeClassProtObjs() {
        //Vector nds = reorderedNds;
        for (int i=0; i<nds.size(); i++) {
            CgenNode cls = (CgenNode)nds.get(i);
            str.print(CgenSupport.WORD); str.println(-1);
            str.print(cls.name + CgenSupport.PROTOBJ_SUFFIX); str.print(CgenSupport.LABEL); //label
            str.print(CgenSupport.WORD); str.println(i); //class tag
            ArrayList<attr> attrs = classAttrs.get(cls.name);
            int objectSize = attrs.size() + 3; // 3 = class tag + object size + disp ref
            str.print(CgenSupport.WORD); str.println(objectSize); //object size
            str.print(CgenSupport.WORD); str.println(cls.name + CgenSupport.DISPTAB_SUFFIX); //dispatch ref
            codeClassAttrs(attrs);
        }
    }
    private void mapClassNamesToTags() {
        for (int i=0; i<nds.size(); i++) {
            CgenNode cls = (CgenNode)nds.get(i);
            classTags.put(cls.name, i);
        }
    }
    private void reassignBasicClassTags() {
        for (int i=0; i<reorderedNds.size(); i++) {
            CgenNode cls = (CgenNode)reorderedNds.get(i);
            if (cls.name == TreeConstants.Str) stringclasstag = i;
            else if (cls.name == TreeConstants.Int) intclasstag = i;
            else if (cls.name == TreeConstants.Bool) boolclasstag = i;
        }
    }
    private void reorderClassNodes() {
        reorderClassNodes(root());
        nds = reorderedNds;
        reassignBasicClassTags();
    }
    private void reorderClassNodes(CgenNode root) {
        if (root == null) return;
        reorderedNds.addElement(root);
        for (Enumeration e = root.getChildren(); e.hasMoreElements(); ) {
            reorderClassNodes((CgenNode)e.nextElement());
        }
    }
    // DEPRECATED
    private void codeClassProtObjs(CgenNode root) {
        if (root == null) return;
        codeClassProtObj(root);
        for (Enumeration e = root.getChildren(); e.hasMoreElements(); ) {
            codeClassProtObjs((CgenNode)e.nextElement());
        }
    }
    // DEPRECATED
    private void codeClassProtObj(CgenNode cls) {
        reorderedNds.addElement(cls);
        str.print(CgenSupport.WORD); str.println(-1); // garbage collector tag
        str.print(cls.name + CgenSupport.PROTOBJ_SUFFIX); str.print(CgenSupport.LABEL); //label
        str.print(CgenSupport.WORD); str.println(classtag); //class tag 
        classtag++;
        ArrayList<attr> attrs = classAttrs.get(cls.name);
        int objectSize = attrs.size() + 3; // 3 = class tag + object size + disp ref
        str.print(CgenSupport.WORD); str.println(objectSize); //object size
        str.print(CgenSupport.WORD); str.println(cls.name + CgenSupport.DISPTAB_SUFFIX); //dispatch ref
        codeClassAttrs(attrs);
    }
    public void emitDefaultObjValue(AbstractSymbol declType) {
        if (declType == TreeConstants.Int) {
            IntSymbol defaultInt = (IntSymbol)AbstractTable.inttable.lookup("0");
            CgenSupport.emitPartialLoadAddress(CgenSupport.ACC, str); defaultInt.codeRef(str); str.println();
        } else if (declType == TreeConstants.Str) {
            StringSymbol defaultStr = (StringSymbol)AbstractTable.stringtable.lookup("");
            CgenSupport.emitPartialLoadAddress(CgenSupport.ACC, str); defaultStr.codeRef(str); str.println();
        } else if (declType == TreeConstants.Bool) {
            CgenSupport.emitPartialLoadAddress(CgenSupport.ACC, str); (new BoolConst(false)).codeRef(str); str.println();
        } else {
            CgenSupport.emitMove(CgenSupport.ACC, CgenSupport.ZERO, str);
        }
    }
    private void codeClassAttrs(ArrayList<attr> attrs) {
        for (attr a : attrs) {
            str.print(CgenSupport.WORD);
            if (a.type_decl == TreeConstants.Int) {
                IntSymbol defaultInt = (IntSymbol)AbstractTable.inttable.lookup("0");
                defaultInt.codeRef(str); str.println();
            } else if (a.type_decl == TreeConstants.Str) {
                StringSymbol defaultStr = (StringSymbol)AbstractTable.stringtable.lookup("");
                defaultStr.codeRef(str); str.println();
            } else if (a.type_decl == TreeConstants.Bool) {
                (new BoolConst(false)).codeRef(str); str.println();
            } else {
                str.println(0);
            }
        }
    }

    private HashSet<AbstractSymbol> getAttrsDefinedInCls(CgenNode cls) {
        HashSet<AbstractSymbol> attrs = new HashSet<AbstractSymbol>();
        Features features = cls.features;
        for (int i=0; i<features.getLength(); i++) {
            Feature f = (Feature)features.getNth(i);
            if (f instanceof attr) {
                attr a = (attr) f;
                attrs.add(a.name);
            }
        }
        return attrs;
    }
    /** Creates data structures representing basic Cool classes (Object,
     * IO, Int, Bool, String).  Please note: as is this method does not
     * do anything useful; you will need to edit it to make if do what
     * you want.
     * */
    private void installBasicClasses() {
        AbstractSymbol filename 
            = AbstractTable.stringtable.addString("<basic class>");

        // A few special class names are installed in the lookup table
        // but not the class list.  Thus, these classes exist, but are
        // not part of the inheritance hierarchy.  No_class serves as
        // the parent of Object and the other special classes.
        // SELF_TYPE is the self class; it cannot be redefined or
        // inherited.  prim_slot is a class known to the code generator.

        addId(TreeConstants.No_class,
                new CgenNode(new class_(0,
                        TreeConstants.No_class,
                        TreeConstants.No_class,
                        new Features(0),
                        filename),
                    CgenNode.Basic, this));

        addId(TreeConstants.SELF_TYPE,
                new CgenNode(new class_(0,
                        TreeConstants.SELF_TYPE,
                        TreeConstants.No_class,
                        new Features(0),
                        filename),
                    CgenNode.Basic, this));

        addId(TreeConstants.prim_slot,
                new CgenNode(new class_(0,
                        TreeConstants.prim_slot,
                        TreeConstants.No_class,
                        new Features(0),
                        filename),
                    CgenNode.Basic, this));

        // The Object class has no parent class. Its methods are
        //        cool_abort() : Object    aborts the program
        //        type_name() : Str        returns a string representation 
        //                                 of class name
        //        copy() : SELF_TYPE       returns a copy of the object

        class_ Object_class = 
            new class_(0, 
                    TreeConstants.Object_, 
                    TreeConstants.No_class,
                    new Features(0)
                    .appendElement(new method(0, 
                            TreeConstants.cool_abort, 
                            new Formals(0), 
                            TreeConstants.Object_, 
                            new no_expr(0)))
                    .appendElement(new method(0,
                            TreeConstants.type_name,
                            new Formals(0),
                            TreeConstants.Str,
                            new no_expr(0)))
                    .appendElement(new method(0,
                            TreeConstants.copy,
                            new Formals(0),
                            TreeConstants.SELF_TYPE,
                            new no_expr(0))),
                    filename);

        installClass(new CgenNode(Object_class, CgenNode.Basic, this));

        // The IO class inherits from Object. Its methods are
        //        out_string(Str) : SELF_TYPE  writes a string to the output
        //        out_int(Int) : SELF_TYPE      "    an int    "  "     "
        //        in_string() : Str            reads a string from the input
        //        in_int() : Int                "   an int     "  "     "

        class_ IO_class = 
            new class_(0,
                    TreeConstants.IO,
                    TreeConstants.Object_,
                    new Features(0)
                    .appendElement(new method(0,
                            TreeConstants.out_string,
                            new Formals(0)
                            .appendElement(new formal(0,
                                    TreeConstants.arg,
                                    TreeConstants.Str)),
                            TreeConstants.SELF_TYPE,
                            new no_expr(0)))
                    .appendElement(new method(0,
                            TreeConstants.out_int,
                            new Formals(0)
                            .appendElement(new formal(0,
                                    TreeConstants.arg,
                                    TreeConstants.Int)),
                            TreeConstants.SELF_TYPE,
                            new no_expr(0)))
                    .appendElement(new method(0,
                            TreeConstants.in_string,
                            new Formals(0),
                            TreeConstants.Str,
                            new no_expr(0)))
                    .appendElement(new method(0,
                                TreeConstants.in_int,
                                new Formals(0),
                                TreeConstants.Int,
                                new no_expr(0))),
            filename);

        installClass(new CgenNode(IO_class, CgenNode.Basic, this));

        // The Int class has no methods and only a single attribute, the
        // "val" for the integer.

        class_ Int_class = 
            new class_(0,
                    TreeConstants.Int,
                    TreeConstants.Object_,
                    new Features(0)
                    .appendElement(new attr(0,
                            TreeConstants.val,
                            TreeConstants.prim_slot,
                            new no_expr(0))),
                    filename);

        installClass(new CgenNode(Int_class, CgenNode.Basic, this));

        // Bool also has only the "val" slot.
        class_ Bool_class = 
            new class_(0,
                    TreeConstants.Bool,
                    TreeConstants.Object_,
                    new Features(0)
                    .appendElement(new attr(0,
                            TreeConstants.val,
                            TreeConstants.prim_slot,
                            new no_expr(0))),
                    filename);

        installClass(new CgenNode(Bool_class, CgenNode.Basic, this));

        // The class Str has a number of slots and operations:
        //       val                              the length of the string
        //       str_field                        the string itself
        //       length() : Int                   returns length of the string
        //       concat(arg: Str) : Str           performs string concatenation
        //       substr(arg: Int, arg2: Int): Str substring selection

        class_ Str_class =
            new class_(0,
                    TreeConstants.Str,
                    TreeConstants.Object_,
                    new Features(0)
                    .appendElement(new attr(0,
                            TreeConstants.val,
                            TreeConstants.Int,
                            new no_expr(0)))
                    .appendElement(new attr(0,
                            TreeConstants.str_field,
                            TreeConstants.prim_slot,
                            new no_expr(0)))
                    .appendElement(new method(0,
                            TreeConstants.length,
                            new Formals(0),
                            TreeConstants.Int,
                            new no_expr(0)))
                    .appendElement(new method(0,
                            TreeConstants.concat,
                            new Formals(0)
                            .appendElement(new formal(0,
                                    TreeConstants.arg, 
                                    TreeConstants.Str)),
                            TreeConstants.Str,
                            new no_expr(0)))
                    .appendElement(new method(0,
                                TreeConstants.substr,
                                new Formals(0)
                                .appendElement(new formal(0,
                                        TreeConstants.arg,
                                        TreeConstants.Int))
                                .appendElement(new formal(0,
                                        TreeConstants.arg2,
                                        TreeConstants.Int)),
                                TreeConstants.Str,
                                new no_expr(0))),
            filename);

        installClass(new CgenNode(Str_class, CgenNode.Basic, this));
    }

    // The following creates an inheritance graph from
    // a list of classes.  The graph is implemented as
    // a tree of `CgenNode', and class names are placed
    // in the base class symbol table.

    private void installClass(CgenNode nd) {
        AbstractSymbol name = nd.getName();
        if (probe(name) != null) return;
        nds.addElement(nd);
        addId(name, nd);
    }

    private void installClasses(Classes cs) {
        for (Enumeration e = cs.getElements(); e.hasMoreElements(); ) {
            installClass(new CgenNode((Class_)e.nextElement(), 
                        CgenNode.NotBasic, this));
        }
    }

    private void buildInheritanceTree() {
        for (Enumeration e = nds.elements(); e.hasMoreElements(); ) {
            setRelations((CgenNode)e.nextElement());
        }
    }

    private void setRelations(CgenNode nd) {
        CgenNode parent = (CgenNode)probe(nd.getParent());
        nd.setParentNd(parent);
        parent.addChild(nd);
    }

    /** Constructs a new class table and invokes the code generator */
    public CgenClassTable(Classes cls, PrintStream str) {
        nds = new Vector();
        classAttrs = new HashMap<AbstractSymbol, ArrayList<attr>>();
        classMethods = new HashMap<AbstractSymbol, ArrayList<Method>>();
        classTags = new HashMap<AbstractSymbol, Integer>();

        this.str = str;

        stringclasstag = 4 /* Change to your String class tag here */;
        intclasstag =    2 /* Change to your Int class tag here */;
        boolclasstag =   3 /* Change to your Bool class tag here */;

        enterScope();
        if (Flags.cgen_debug) System.out.println("Building CgenClassTable");

        installBasicClasses();
        installClasses(cls);
        if (Flags.cgen_debug) {
            for(int i=0; i<nds.size(); i++) {
                CgenNode n = (CgenNode) nds.get(i);
                System.out.println("index: " + i);
                System.out.println(n.name);
            }
        }
        buildInheritanceTree();
        reorderClassNodes();
        mapClassNamesToTags();
        buildClassMethods();
        buildClassAttrs();
        code();

        exitScope();
    }

    /** This method is the meat of the code generator.  It is to be
      filled in programming assignment 5 */
    public void code() {
        if (Flags.cgen_debug) System.out.println("coding global data");
        codeGlobalData();

        if (Flags.cgen_debug) System.out.println("choosing gc");
        codeSelectGc();

        if (Flags.cgen_debug) System.out.println("coding constants");
        codeConstants();
        //codeClassProtObjs(root());
        codeClassNameTab();
        codeClassObjTab();
        codeClassDispTab();
        codeClassProtObjs();
        //                 Add your code to emit
        //                   - prototype objects
        //                   - class_nameTab
        //                   - class_objTab
        //                   - dispatch tables

        if (Flags.cgen_debug) System.out.println("coding global text");
        codeGlobalText();
        codeObjInitializers();
        codeClassMethods();
        //                 Add your code to emit
        //                   - object initializer
        //                   - the class methods
        //                   - etc...
    }
    public void codeClassMethods() {
        for (int i=0; i<nds.size(); i++) {
            CgenNode cls = (CgenNode)nds.get(i);
            if (cls.basic()) continue;
            ArrayList<Method> methods = classMethods.get(cls.name);
            for (Method method : methods) {
                if (method.cls != cls.name) continue;
                //method.code(str, cls, this);
                codeMethod(cls, method);
            }
        }
    }

    public void codeMethod(CgenNode cls, Method method) {
        Formals formals = method.m.formals;
        Expression expr = method.m.expr;
        int numArgs = formals.getLength();
        emitLabel(cls.name + CgenSupport.METHOD_SEP + method.name);
        pushFrame();
        moveAccToSelf();
        expr.code(str, cls.name, method.name, this);
        popFrame(numArgs);
        emitReturn();
    }
    public void codeObjInitializers() {
        for (int i=0; i<nds.size(); i++) {
            CgenNode cls = (CgenNode)nds.get(i);
            emitLabel(cls.name + CgenSupport.CLASSINIT_SUFFIX);
            pushFrame();
            moveAccToSelf();
            codeParentInit(cls);
            codeAttrsInit(cls);
            moveSelfToAcc();
            popFrame(0);
            emitReturn();
        }
    }
    public void emitLabel(String name) {
        str.print(name + CgenSupport.LABEL);
    }
    public void emitLabel() {
        CgenSupport.emitLabelDef(labelCount, str);
        labelCount++;
    }
    public void emitReturn() {
        CgenSupport.emitReturn(str);
    }
    public void moveAccToSelf() {
        CgenSupport.emitMove(CgenSupport.SELF, CgenSupport.ACC, str);
    }
    public void moveSelfToAcc() {
        CgenSupport.emitMove(CgenSupport.ACC, CgenSupport.SELF, str);
    }
    public void pushFrame() {
        CgenSupport.emitAddiu(CgenSupport.SP, CgenSupport.SP, -12, str);
        CgenSupport.emitStore(CgenSupport.FP, 3, CgenSupport.SP, str);
        CgenSupport.emitStore(CgenSupport.SELF, 2, CgenSupport.SP, str);
        CgenSupport.emitStore(CgenSupport.RA, 1, CgenSupport.SP, str);
        CgenSupport.emitAddiu(CgenSupport.FP, CgenSupport.SP, 4, str);
    }

    public void popFrame(int numArgs) {
        CgenSupport.emitLoad(CgenSupport.FP, 3, CgenSupport.SP, str);
        CgenSupport.emitLoad(CgenSupport.SELF, 2, CgenSupport.SP, str);
        CgenSupport.emitLoad(CgenSupport.RA, 1, CgenSupport.SP, str);
        CgenSupport.emitAddiu(CgenSupport.SP, CgenSupport.SP, 12 + CgenSupport.WORD_SIZE * numArgs, str);
    }
    public void emitArithOperation(Expression e1, Expression e2, AbstractSymbol so, AbstractSymbol method, String op) {
        e1.code(str, so, method, this);                                                 // e1 stores result in Int object pointed by $a0
        CgenSupport.emitLoad(CgenSupport.ACC, 3, CgenSupport.ACC, str);                 // extract Int value from Int object 
        emitPushAccToStack();                                                           // push Int value to stack
        e2.code(str, so, method, this);                                                 // e1 stores result in Int object pointed by $a0
        CgenSupport.emitJal("Object.copy", str);                                        // make a copy of second Int object 
        CgenSupport.emitLoad(CgenSupport.T2, 3, CgenSupport.ACC, str);                  // extract Int value from the copied second Int object into $t2
        emitLoadT1(CgenSupport.SP, 1);                                                  // extract Int value of the first Int object into $t1
        str.println(op + CgenSupport.T1 + " " + CgenSupport.T1 + " " + CgenSupport.T2); // perform operation
        CgenSupport.emitStore(CgenSupport.T1, 3, CgenSupport.ACC, str);                 // store result into the copied Int object, pointed by $a0
        emitPopStack();
    }
    public void emitPopStack() {
        CgenSupport.emitAddiu(CgenSupport.SP, CgenSupport.SP, 4, str);
    }
    public void emitAbort(String abortType) {
        emitLoadAccFilename();
        // to be completed
    }
    public void emitLoadAccFilename() {
        CgenNode cls = (CgenNode) lookup(TreeConstants.Main);
        StringSymbol filename = (StringSymbol)AbstractTable.stringtable.lookup(cls.filename.toString());
        CgenSupport.emitPartialLoadAddress(CgenSupport.ACC, str); filename.codeRef(str); str.println();
    }
    public void emitLoadAccFalse() {
        CgenSupport.emitPartialLoadAddress(CgenSupport.ACC, str); (new BoolConst(false)).codeRef(str); str.println();
    }
    public void emitLoadAccTrue() {
        CgenSupport.emitPartialLoadAddress(CgenSupport.ACC, str); (new BoolConst(true)).codeRef(str); str.println();
    }
    public void emitLoadT1(String pointer, int offset) {
        CgenSupport.emitLoad(CgenSupport.T1, offset, pointer, str);
    }
    public void emitStoreAcc(String pointer, int offset) {
        CgenSupport.emitStore(CgenSupport.ACC, offset, pointer, str);
    }
    public void emitLoadAcc(String pointer, int offset) {
        CgenSupport.emitLoad(CgenSupport.ACC, offset, pointer, str);
    }

    public void emitPushAccToStack() {
        CgenSupport.emitStore(CgenSupport.ACC, 0, CgenSupport.SP, str);
        CgenSupport.emitAddiu(CgenSupport.SP, CgenSupport.SP, -4, str);
    }
    public void codeParentInit(CgenNode cls) {
        AbstractSymbol parentName = cls.getParentNd().getName();
        if (parentName == TreeConstants.No_class) return;
        CgenSupport.emitJal(parentName + CgenSupport.CLASSINIT_SUFFIX, str);
    }

    public void codeAttrsInit(CgenNode cls) {
        ArrayList<attr> attrs = classAttrs.get(cls.name);
        HashSet<AbstractSymbol> definedAttrs = getAttrsDefinedInCls(cls);
        for (int i=0; i<attrs.size(); i++) {
            attr a = attrs.get(i);
            if (a.init.get_type() != null && definedAttrs.contains(a.name)) { // only init attrs that are defined in current class
                a.init.code(str, cls.name, null, this); // emit code for attr initializers
                int offset = CgenSupport.DEFAULT_OBJFIELDS + i;
                CgenSupport.emitStore(CgenSupport.ACC, offset, CgenSupport.SELF, str); // store init value in object on heap
                if(Flags.cgen_Memmgr != Flags.GC_NOGC) {
                    CgenSupport.emitAddiu(CgenSupport.A1, CgenSupport.SELF, offset * CgenSupport.WORD_SIZE, str);
                    CgenSupport.emitJal("_GenGC_Assign", str);
                }
            }
        }
    }

    /** Gets the root of the inheritance tree */
    public CgenNode root() {
        return (CgenNode)probe(TreeConstants.Object_);
    }
    public int getMaxDynamicClassTag(int staticClassTag) {
        CgenNode cls = (CgenNode) nds.get(staticClassTag);
        return getNumberOfChildren(cls) + staticClassTag;
    }

    public int getNumberOfChildren(CgenNode root) {
        if (root == null) return -1;
        int numOfChildren = 0;
        for (Enumeration e = root.getChildren(); e.hasMoreElements(); ) {
            numOfChildren += getNumberOfChildren((CgenNode)e.nextElement()) + 1;
        }
        return numOfChildren;
    }
    public ArrayList<Branch> sortBranches(Cases cases) {
        ArrayList<Branch> branches = new ArrayList<Branch>();
        for (int i=0; i<cases.getLength(); i++) {
            branch b = (branch)cases.getNth(i);
            branches.add(new Branch(b, this));
        }
        //System.out.println("---");
        Collections.sort(branches);
        /*
        for (Branch b : branches) {
            System.out.printf("branch name: %s, static: %d, maxDynamic: %d\n", b.b.name, b.staticClassTag, b.maxDynamicClassTag);
        }
        */
        return branches;
    }
}

class Method {
    public AbstractSymbol name;
    public AbstractSymbol cls;
    public method m;
    public Method(AbstractSymbol name, AbstractSymbol cls, method m) {
        this.name = name;
        this.cls = cls;
        this.m = m;
    }
}

class Location {
    public String pointer;
    public int offset;
    public Location(String p, int o) {
        pointer = p;
        offset = o;
    }
}

class Branch implements Comparable<Branch> {
    public branch b;
    public int staticClassTag;
    public int maxDynamicClassTag;
    public Branch(branch b, CgenClassTable cTable) {
        this.b = b;
        this.staticClassTag = cTable.getClassTag(b.type_decl);
        this.maxDynamicClassTag = cTable.getMaxDynamicClassTag(this.staticClassTag);
    }
    public int compareTo(Branch other) {
        Integer thisRange = Math.abs(this.maxDynamicClassTag - this.staticClassTag);
        Integer otherRange = Math.abs(other.maxDynamicClassTag - other.staticClassTag);
        return thisRange.compareTo(otherRange);
    }
}

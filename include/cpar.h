//==========================================================================
//   CPAR.H   - header for
//                             OMNeT++
//            Discrete System Simulation in C++
//
//
//  Declaration of the following classes:
//    cPar       : general value holding class
//    cModulePar : specialized cPar that serves as module parameter
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2001 Andras Varga
  Technical University of Budapest, Dept. of Telecommunications,
  Stoczek u.2, H-1111 Budapest, Hungary.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __CPAR_H
#define __CPAR_H

#include "cobject.h"

#define SHORTSTR  27

#define NOPAR  NO(cPar)

//=== classes declared here:
class  cPar;
class  cModulePar;

//=== class mentioned
class  cStatistic;

//==========================================================================

/**
 * sXElem: one component in a (reversed Polish) expression in a cPar.
 *
 * If the value of the cPar is of expression type, the expression
 * must be converted to reversed Polish form. The reversed Polish
 * form expression is stored in a vector of sXElem structures.
 * sXElem is not a descendant of cObject.
 *
 * NOTE: not a cObject descendant!
 *
 */
struct sXElem
{
    // Type chars:
    //   D  double constant
    //   P  pointer to "external" cPar (owned by someone else)
    //   R  "reference": the cPar will be dup()'ped and the copy kept
    //   0/1/2/3  function with 0/1/2/3 arguments
    //   @  math operator (+-*%/^=!<{>}?); see cPar::evaluate()
    //
    char type;    // D/P/R/0/1/2/3/@ (see above)
    union {
        double d;           // D
        cPar *p;            // P/R
        MathFuncNoArg f0;   // 0
        MathFunc1Arg  f1;   // 1
        MathFunc2Args f2;   // 2
        MathFunc3Args f3;   // 3
        char op;            // @, op = +-*/%^=!<{>}?
    };

    /**
     * Effect during evaluation of the expression: pushes the given number
     * (which is converted to double) on the evaluation stack.
     */
    void operator=(int _i)            {type='D'; d=_i;  }

    /**
     * Effect during evaluation of the expression: pushes the given number
     * (which is converted to double) on the evaluation stack.
     */
    void operator=(long _l)           {type='D'; d=_l;  }

    /**
     * Effect during evaluation of the expression: pushes the given number
     * (which is converted to double) on the evaluation stack.
     */
    void operator=(double _d)         {type='D'; d=_d;  }

    /**
     * Effect during evaluation of the expression: takes the value of
     * the cPar object  (a double) and pushes the value
     * on the evaluation stack. The cPar is an "external"
     * one: its ownership does not change. This is how NED-language REF
     * parameters in expressions are handled.
     */
    void operator=(cPar *_p)          {type='P'; p=_p;  }

    /**
     * Effect during evaluation of the expression: takes the value of
     * the cPar object  (a double) and pushes the value
     * on the evaluation stack. The cPar which evaluates this
     * expression will copy the cPar for itself.
     */
    void operator=(cPar& _r);         //{type='R'; p=(cPar *)_r.dup();} See after cPar!

    /**
     * The argument can be a pointer to a function that takes (0, 1,
     * 2, or 3) double arguments and returns a double
     * (e.g. sqrt()). Effect during evaluation of the expression:
     * the given number of doubles are popped from the stack,
     * the given function is called with them as arguments, and the return
     * value is pushed back on the stack. See also the cFunctionType
     * class and the Define_Function() macro.
     *
     * The OMNeT++ functions generating random variables of different
     * distributions can also be used in sXElem expressions.
     */
    void operator=(MathFuncNoArg _f)  {type='0'; f0=_f;}

    /**
     * The argument can be a pointer to a function that takes (0, 1,
     * 2, or 3) double arguments and returns a double
     * (e.g. sqrt()). Effect during evaluation of the expression:
     * the given number of doubles are popped from the stack,
     * the given function is called with them as arguments, and the return
     * value is pushed back on the stack. See also the cFunctionType
     * class and the Define_Function() macro.
     *
     * The OMNeT++ functions generating random variables of different
     * distributions can also be used in sXElem expressions.
     */
    void operator=(MathFunc1Arg  _f)  {type='1'; f1=_f;}

    /**
     * The argument can be a pointer to a function that takes (0, 1,
     * 2, or 3) double arguments and returns a double
     * (e.g. sqrt()). Effect during evaluation of the expression:
     * the given number of doubles are popped from the stack,
     * the given function is called with them as arguments, and the return
     * value is pushed back on the stack. See also the cFunctionType
     * class and the Define_Function() macro.
     *
     * The OMNeT++ functions generating random variables of different
     * distributions can also be used in sXElem expressions.
     */
    void operator=(MathFunc2Args _f)  {type='2'; f2=_f;}

    /**
     * The argument can be a pointer to a function that takes (0, 1,
     * 2, or 3) double arguments and returns a double
     * (e.g. sqrt()). Effect during evaluation of the expression:
     * the given number of doubles are popped from the stack,
     * the given function is called with them as arguments, and the return
     * value is pushed back on the stack. See also the cFunctionType
     * class and the Define_Function() macro.
     *
     * The OMNeT++ functions generating random variables of different
     * distributions can also be used in sXElem expressions.
     */
    void operator=(MathFunc3Args _f)  {type='3'; f3=_f;}

    /**
     * Operation. During evaluation of the expression, two items (or three,
     * with '?') are popped out of the stack, the given operator
     * is applied to them and the result is pushed back on the stack.
     *
     * The operation can be:
     * <UL>
     *   <LI> + - * / add, subtract, multiply, divide
     *   <LI> % ^  modulo, power of
     *   <LI> = !  equal, not equal
     *   <LI> > }  greater, greater or equal
     *   <LI> < {  less, less or equal
     *   <LI> ?  inline if (the C/C++ ?: operator)
     * </UL>
     */
    void operator=(char _op)          {type='@'; op=_op;}
};

//==========================================================================

/**
 * Parameter class are designed to hold a value. Many types are
 * available:
 *
 * Types and type characters:
 *
 * <UL>
 *   <LI> basic types: C char, S string, L long, D double
 *   <LI> F math function (MathFuncNoArgs,MathFunc1Args,etc),
 *   <LI> X expression (table of sXElems),
 *   <LI> T distribution from a cStatistic,
 *   <LI> P pointer to cObject,
 *   <LI> I indirection (refers to another cPar)
 * </UL>
 *
 * For all types, an input flag can be set. In this case,
 * the user will be asked to enter the value when the object's value
 * is first used. The prompt string can also be specified
 * for cPar. If no prompt string is given, the object's
 * name will be displayed as prompt text.
 *
 * NOTE: forEach() ignores objects stored here such as cPars in eXpressions,
 * cObject pointed to in 'P' type, cStatistic in disTribution type
 */
class SIM_API cPar : public cObject
{
  protected:
    static char *possibletypes;
  private:
    char typechar;     // S/B/L/D/F/T/X/P/O/I
    bool inputflag;
    bool changedflag;
    opp_string promptstr; // prompt text used when the value is being input

    union {    // Take care of 'operator=()' when changing this!!!
       struct { bool sht; char *str;            } ls;   // S:long string
       struct { bool sht; char str[SHORTSTR+1]; } ss;   // S:short str
       struct { long val;                       } lng;  // L:long,B:bool
       struct { double val;                     } dbl;  // D:double
       struct { MathFunc f; int argc;
                double p1,p2,p3;                } func; // F:math function
       struct { cStatistic *res;                } dtr;  // T:distribution
       struct { sXElem *xelem; int n;           } expr; // X:expression
       struct { cPar *par;                      } ind;  // I:indirection
       struct { void *ptr;
                VoidDelFunc delfunc;
                VoidDupFunc dupfunc;
                size_t itemsize;                } ptr;  // P:void* pointer
       struct { cObject *obj;                   } obj;  // O:object pointer
    };

  private:
    // helper func: destruct old value
    void deleteold();

    // helper func: evaluates expression (X)
    double evaluate();

    // helper func: rand.num with given distr.(T)
    double fromstat();

    // setFromText() helper func.
    bool setfunction(char *w);

  protected:
    /**
     * Hook function, called each time just before the value of this object changes.
     * This default implementation does nothing.
     */
    virtual void valueChanges();

  public:
    /** @name Constructors, destructor, assignment. */
    //@{

    /**
     * Copy constructor, creates an exact copy of the argument.
     */
    cPar(cPar& other);

    /**
     * Constructor, creates a cPar with the given name and long
     * ('L') as default type.
     */
    explicit cPar(const char *name=NULL);

    /**
     * Constructor, creates a copy of the second argument with another
     * name.
     */
    explicit cPar(const char *name, cPar& other);

    /**
     * Destructor.
     */
    virtual ~cPar();

    /**
     * The assignment operator works with cPar objects.
     */
    cPar& operator=(cPar& otherpar);
    //@}

    /** @name Redefined cObject member functions */
    //@{

    /**
     * Returns pointer to the class name string, "cPar".
     */
    virtual const char *className() const {return "cPar";}

    /**
     * Duplicates the object and returns a pointer to the new one.
     */
    virtual cObject *dup()   {return new cPar(*this);}

    /**
     * MISSINGDOC: cPar:void info(char*)
     */
    virtual void info(char *buf);

    /**
     * MISSINGDOC: cPar:char*inspectorFactoryName()
     */
    virtual const char *inspectorFactoryName() const {return "cParIFC";}

    /**
     * MISSINGDOC: cPar:void writeContents(ostream&)
     */
    virtual void writeContents(ostream& os);

    /**
     * MISSINGDOC: cPar:int netPack()
     */
    virtual int netPack();

    /**
     * MISSINGDOC: cPar:int netUnpack()
     */
    virtual int netUnpack();
    //@}

    /** @name Setter functions. Note that overloaded assignment operators also exist. */
    //@{

    /**
     * FIXME: new functions:
     */
    cPar& setBoolValue(bool b);              // bool

    /**
     * MISSINGDOC: cPar:cPar&setLongValue(long)
     */
    cPar& setLongValue(long l);              // long

    /**
     * MISSINGDOC: cPar:cPar&setStringValue(char*)
     */
    cPar& setStringValue(const char *s);     // string

    /**
     * MISSINGDOC: cPar:cPar&setDoubleValue(double)
     */
    cPar& setDoubleValue(double d);          // double

    /**
     * MISSINGDOC: cPar:cPar&setDoubleValue(cStatistic*)
     */
    cPar& setDoubleValue(cStatistic *res);   // distribution

    /**
     * MISSINGDOC: cPar:cPar&setDoubleValue(sXElem*,int)
     */
    cPar& setDoubleValue(sXElem *x, int n);  // expression

    /**
     * MISSINGDOC: cPar:cPar&setDoubleValue(MathFuncNoArg)
     */
    cPar& setDoubleValue(MathFuncNoArg f);   // math func: 0,1,2,3 args

    /**
     * MISSINGDOC: cPar:cPar&setDoubleValue(MathFunc1Arg,double)
     */
    cPar& setDoubleValue(MathFunc1Arg  f, double p1);

    /**
     * MISSINGDOC: cPar:cPar&setDoubleValue(MathFunc2Args,double,double)
     */
    cPar& setDoubleValue(MathFunc2Args f, double p1, double p2);

    /**
     * MISSINGDOC: cPar:cPar&setDoubleValue(MathFunc3Args,double,double,double)
     */
    cPar& setDoubleValue(MathFunc3Args f, double p1, double p2, double p3);

    /**
     * MISSINGDOC: cPar:cPar&setPointerValue(void*)
     * See also configPointer()!
     */
    cPar& setPointerValue(void *ptr);        // void* pointer

    /**
     * MISSINGDOC: cPar:cPar&setObjectValue(cObject*)
     * See cObject's ownership control: takeOwnership() flag, etc.
     */
    cPar& setObjectValue(cObject *obj);      // cObject* pointer

    /**
     * Configures memory management for the void* pointer ('P') type.
     * Similar to cLinkedList's configPointer() function.
     * <TABLE BORDER=1>
     * <TR><TD WIDTH=96><B>delete func.</B></TD><TD WIDTH=89><B>dupl.func.</B>
     * </TD><TD WIDTH=92><B>itemsize</B></TD><TD WIDTH=317><B>behaviour</B>
     * </TD></TR>
     * <TR><TD WIDTH=96>NULL</TD><TD WIDTH=89>NULL
     * </TD><TD WIDTH=92>0</TD><TD WIDTH=317>Pointer is treated as mere pointer - no memory management. Duplication copies the pointer, and deletion does nothing.
     * </TD></TR>
     * <TR><TD WIDTH=96>NULL</TD><TD WIDTH=89>NULL
     * </TD><TD WIDTH=92>>0 size</TD><TD WIDTH=317>Plain memory management. Duplication is done with new char[size]+memcpy(), and deletion is done via delete.
     * </TD></TR>
     * <TR><TD WIDTH=96>NULL or user's delete func.</TD><TD WIDTH=89>user's dupfunc.
     * </TD><TD WIDTH=92>indifferent</TD><TD WIDTH=317>Sophisticated memory management. Duplication is done by calling the user-supplied duplication function, which should do the allocation and the appropriate copying. Deletion is done by calling the user-supplied delete function, or the delete operator if it is not supplied.
     * </TD></TR>
     * </TABLE>
     */
    void configPointer( VoidDelFunc delfunc, VoidDupFunc dupfunc, size_t itemsize=0);
    //@}

    /** @name Getter functions. Note that overloaded conversion operators also exist. */
    //@{

    /**
     * FIXME: functions to return value
     *   conversion operators also exist; see later
     */
    bool   boolValue();

    /**
     * Returns value as long. Converts from types long (L),
     * double (D), Boolean (B), function (F), distribution (T) and expression
     * (X).
     */
    long   longValue();

    /**
     * Returns value as char*. Only for string (S) type.
     */
    const char *stringValue();

    /**
     * Returns value as double. Converts from types long (L),
     * double (D), function (F), Boolean (B), distribution (T) and expression
     * (X).
     */
    double doubleValue();

    /**
     * Returns value as pointer to cObject. Type must be pointer
     * (P).
     */
    void *pointerValue();

    /**
     * Returns value as pointer to cObject. Type must be pointer
     * (O).
     */
    cObject *objectValue();
    //@}

    /** @name Redirection */
    //@{

    /**
     * TBD
     */
    cPar& setRedirection(cPar *par);

    /**
     * TBD
     */
    bool isRedirected() {return typechar=='I';}

    /**
     * Returns NULL if the cPar's value is not redirected
     * to another cPar; otherwise it returns the pointer of
     * that cPar. This function is the only way to determine
     * if an object is redirected or not (type() returns the
     * type of the other cPar: 'D', 'L' etc).
     */
    cPar *redirection();

    /**
     * Break the redirection. The new type will be long ('L').
     */
    void cancelRedirection();
    //@}

    /** @name Type, prompt text, input flag, change flag. */
    //@{

    /**
     * Returns type character. If the "real" type is 'I',
     * it returns the type of the object it is redirected to (for example,
     * 'D', 'L', etc.)
     */
    char type();                   // type char

    /**
     * Returns true if the stored value is of a numeric type.
     */
    bool isNumeric();

    /**
     * Returns the prompt text or NULL.
     */
    const char *prompt();          // prompt text

    /**
     * Sets the prompt text.
     */
    void setPrompt(const char *s); // set prompt str

    /**
     * Sets (ip=true) or clears (ip=false) the input
     * flag.
     */
    void setInput(bool ip);        // set input flag

    /**
     * Returns true if the parameter is of input type (the input
     * flag is set).
     */
    bool isInput();                // is an input value?

    /**
     * Returns true if the value has changed since the last
     * changed() call. (It clears the 'changed' flag.)
     */
    bool changed();
    //@}

    /** @name Utility functions. */
    //@{

    /**
     * Reads the object value from the ini file or from the user.
     */
    cPar& read();

    /**
     * Replaces the object value with its evaluation (a double).
     * Implemented as something like setValue('D', this->doubleValue()).
     */
    void convertToConst();

    /**
     * Compares the stored values. The two objects must have the same type character
     * and the same value to be equal.
     */
    bool equalsTo(cPar *par);
    //@}

    /** @name Convert to/from text representation.
    //@{

    /**
     * Places the value in text format it into buffer buf which
     * is maxlen characters long.
     */
    virtual void getAsText(char *buf, int maxlen);

    /**
     * This function tries to interpret the argument text as a type
     * typed value. type=='?' means that the type is to be auto-selected.
     * On success, cPar is updated with the new value and true
     * is returned, otherwise the function returns false. No
     * error message is generated.
     */
    virtual bool setFromText(const char *text, char tp);
    //@}

    /** @name Overloaded assignment and conversion operators. */
    //@{

    /**
     * Equivalent to setBoolValue().
     */
    cPar& operator=(bool b)          {return setBoolValue(b);}

    /**
     * Equivalent to setStringValue().
     */
    cPar& operator=(const char *s)   {return setStringValue(s);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(char c)          {return setLongValue((long)c);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(unsigned char c) {return setLongValue((long)c);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(int i)           {return setLongValue((long)i);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(unsigned int i)  {return setLongValue((long)i);}

    /**
     * Equivalent to setLongValue().
     */
    cPar& operator=(long l)          {return setLongValue(l);}

    /**
     * Converts the argument to long, and calls setLongValue().
     */
    cPar& operator=(unsigned long l) {return setLongValue((long)l);}

    /**
     * Equivalent to setDoubleValue().
     */
    cPar& operator=(double d)        {return setDoubleValue(d);}

    /**
     * Converts the argument to double, and calls setDoubleValue().
     */
    cPar& operator=(long double d)   {return setDoubleValue((double)d);}

    /**
     * Equivalent to setPointerValue().
     */
    cPar& operator=(void *ptr)       {return setPointerValue(ptr);}

    /**
     * Equivalent to setObjectValue().
     */
    cPar& operator=(cObject *obj)    {return setObjectValue(obj);}

    /**
     * Equivalent to boolValue().
     */
    operator bool()          {return boolValue();}

    /**
     * Equivalent to stringValue().
     */
    operator const char *()  {return stringValue();}

    /**
     * Calls longValue() and converts the result to char.
     */
    operator char()          {return (char)longValue();}

    /**
     * Calls longValue() and converts the result to unsigned char.
     */
    operator unsigned char() {return (unsigned char)longValue();}

    /**
     * Calls longValue() and converts the result to int.
     */
    operator int()           {return (int)longValue();}

    /**
     * Calls longValue() and converts the result to unsigned int.
     */
    operator unsigned int()  {return (unsigned int)longValue();}

    /**
     * Equivalent to longValue().
     */
    operator long()          {return longValue();}

    /**
     * Calls longValue() and converts the result to unsigned long.
     */
    operator unsigned long() {return longValue();}

    /**
     * Equivalent to doubleValue().
     */
    operator double()        {return doubleValue();}

    /**
     * Calls doubleValue() and converts the result to long double.
     */
    operator long double()   {return doubleValue();}

    /**
     * Equivalent to pointerValue().
     */
    operator void *()        {return pointerValue();}

    /**
     * Equivalent to objectValue().
     */
    operator cObject *()     {return objectValue();}
    //@}

    /** @name Compare function */
    //@{

    /**
     * Compares two cPars by their value if they are numeric.
     * This function can be used to sort cPar objects in a priority
     * queue.
     */
    static int cmpbyvalue(cObject *one, cObject *other);
    //@}
};

// this function cannot be defined within sXElem because of declaration order
inline void sXElem::operator=(cPar& _r)  {type='R'; p=(cPar *)_r.dup();}

//==========================================================================

/**
 * Module parameter object. This is specialized version of cPar: it is capable
 * of logging parameter changes to file.
 *
 * NOTE: dup() creates only a cPar, NOT a cModulePar!
 */
class SIM_API cModulePar : public cPar
{
    friend class cModule;
  private:
    cModule *omodp;        // owner module
    bool log_initialised;  // logging: true if the "label" line already written out
    long log_ID;           // logging: ID of the data lines
    simtime_t lastchange;  // logging: time of last value change

  private:
    // helper function
    void _construct();

  public:
    /** @name Constructors, destructor, assignment. */
    //@{

    /**
     * Constructor.
     */
    cModulePar(cPar& other);

    /**
     * Constructor.
     */
    explicit cModulePar(const char *name=NULL);

    /**
     * Constructor.
     */
    explicit cModulePar(const char *name, cPar& other);

    /**
     * Destructor.
     */
    virtual ~cModulePar();

    /**
     * MISSINGDOC: cModulePar:cModulePar&operator=(cModulePar&)
     */
    cModulePar& operator=(cModulePar& otherpar);
    //@}

    /** @name Redefined cObject member functions */
    //@{

    /**
     * FIXME: redefined functions
     */
    virtual const char *className() const {return "cModulePar";}

    /**
     * MISSINGDOC: cModulePar:cObject*dup()
     */
    virtual cObject *dup()  {return new cPar(*this);}

    /**
     * MISSINGDOC: cModulePar:char*inspectorFactoryName()
     */
    virtual const char *inspectorFactoryName() const {return "cModuleParIFC";}

    /**
     * MISSINGDOC: cModulePar:char*fullPath()
     */
    virtual const char *fullPath() const;
    //@}

    /** @name Redefined cPar functions. */
    //@{

    /**
     * FIXME: Does parameter logging.
     */
    virtual void valueChanges();
    //@}

    /** @name Set/get owner module. */
    //@{

    /**
     * FIXME: new functions
     */
    void setOwnerModule(cModule *om)   {omodp=om;}

    /**
     * MISSINGDOC: cModulePar:cModule*ownerModule()
     */
    cModule *ownerModule()             {return omodp;}
    //@}
};


//=== operators dealing with cPars
//
// These operators were removed -- see ChangeLog, Jan 17 2000 entry.
//

//#ifndef NO_CPAR_OPERATIONS
//#ifdef  LONG_CPAR_OPERATIONS
// inline int operator<(cPar& p, cPar& q)  {return (long)p<(long)q;}
// inline int operator<(long d, cPar& p)   {return d<(long)p;}
// inline int operator<(cPar& p, long d)   {return (long)p<d;}
// inline int operator>(cPar& p, cPar& q)  {return (long)p>(long)q;}
// inline int operator>(long d, cPar& p)   {return d>(long)p;}
// inline int operator>(cPar& p, long d)   {return (long)p>d;}
//
// inline int operator<=(cPar& p, cPar& q) {return (long)p<=(long)q;}
// inline int operator<=(long d, cPar& p)  {return d<=(long)p;}
// inline int operator<=(cPar& p, long d)  {return (long)p<=d;}
// inline int operator>=(cPar& p, cPar& q) {return (long)p>=(long)q;}
// inline int operator>=(long d, cPar& p)  {return d>=(long)p;}
// inline int operator>=(cPar& p, long d)  {return (long)p>=d;}
//
// inline long operator+(cPar& p, cPar& q) {return (long)p+(long)q;}
// inline long operator+(long d, cPar& p)  {return d+(long)p;}
// inline long operator+(cPar& p, long d)  {return (long)p+d;}
// inline long operator-(cPar& p, cPar& q) {return (long)p-(long)q;}
// inline long operator-(long d, cPar& p)  {return d-(long)p;}
// inline long operator-(cPar& p, long d)  {return (long)p-d;}
//
// inline long operator*(cPar& p, cPar& q)  {return (long)p*(long)q;}
// inline long operator*(long d, cPar& p)   {return d*(long)p;}
// inline long operator*(cPar& p, long d)   {return (long)p*d;}
// inline long operator/(cPar& p, cPar& q)  {return (long)p/(long)q;}
// inline long operator/(long d, cPar& p)   {return d/(long)p;}
// inline long operator/(cPar& p, long d)   {return (long)p/d;}
//
//#else
//
// inline int operator<(cPar& p, cPar& q)  {return (double)p<(double)q;}
// inline int operator<(double d, cPar& p) {return d<(double)p;}
// inline int operator<(cPar& p, double d) {return (double)p<d;}
// inline int operator>(cPar& p, cPar& q)  {return (double)p>(double)q;}
// inline int operator>(double d, cPar& p) {return d>(double)p;}
// inline int operator>(cPar& p, double d) {return (double)p>d;}
//
// inline int operator<=(cPar& p, cPar& q)  {return (double)p<=(double)q;}
// inline int operator<=(double d, cPar& p) {return d<=(double)p;}
// inline int operator<=(cPar& p, double d) {return (double)p<=d;}
// inline int operator>=(cPar& p, cPar& q)  {return (double)p>=(double)q;}
// inline int operator>=(double d, cPar& p) {return d>=(double)p;}
// inline int operator>=(cPar& p, double d) {return (double)p>=d;}
//
// inline double operator+(cPar& p, cPar& q)  {return (double)p+(double)q;}
// inline double operator+(double d, cPar& p) {return d+(double)p;}
// inline double operator+(cPar& p, double d) {return (double)p+d;}
// inline double operator-(cPar& p, cPar& q)  {return (double)p-(double)q;}
// inline double operator-(double d, cPar& p) {return d-(double)p;}
// inline double operator-(cPar& p, double d) {return (double)p-d;}
//
// inline double operator*(cPar& p, cPar& q)  {return (double)p*(double)q;}
// inline double operator*(double d, cPar& p) {return d*(double)p;}
// inline double operator*(cPar& p, double d) {return (double)p*d;}
// inline double operator/(cPar& p, cPar& q)  {return (double)p/(double)q;}
// inline double operator/(double d, cPar& p) {return d/(double)p;}
// inline double operator/(cPar& p, double d) {return (double)p/d;}
//#endif
//#endif

#endif

//=========================================================================
//  STDDEV.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//  Author: Tamas Borbely
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2015 Andras Varga
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_STDDEV_H
#define __OMNETPP_STDDEV_H

#include "commonnodes.h"
#include "xyarray.h"

NAMESPACE_BEGIN


/**
 * Calculates min, max, mean, stddev.
 */
class SCAVE_API StddevNode : public SingleSinkNode
{
    protected:
         long numValues;
         double minValue, maxValue;
         double sumValues, sqrsumValues;

    protected:
        virtual void collect(double val);

    public:
        StddevNode();
        virtual ~StddevNode();
        virtual bool isReady() const;
        virtual void process();
        virtual bool isFinished() const;

        virtual long getCount() const     {return numValues;}
        virtual double getSum() const     {return sumValues;}
        virtual double getSqrSum() const  {return sqrsumValues;}
        virtual double getMin() const     {return minValue;}
        virtual double getMax() const     {return maxValue;}
        virtual double getMean() const    {return numValues ? sumValues/numValues : 0.0;}
        virtual double getStddev() const;
        virtual double getVariance() const;
};


class SCAVE_API StddevNodeType : public SingleSinkNodeType
{
    public:
        virtual const char *getName() const {return "stddev";}
        virtual const char *getDescription() const;
        virtual bool isHidden() const {return true;}
        virtual void getAttributes(StringMap& attrs) const;
        virtual Node *create(DataflowManager *mgr, StringMap& attrs) const;
};

NAMESPACE_END


#endif



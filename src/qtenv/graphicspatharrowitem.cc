#include "graphicspatharrowitem.h"

#include <QPen>
#include <QPainter>

void GraphicsPathArrowItem::paint(QPainter *painter, const QStyleOptionGraphicsItem *option, QWidget *widget)
{
//    auto polygons = path.toSubpathPolygons();
//    qreal lineWidth = pen().widthF();
//    switch(path.elementAt(1).type)   //0 element is QPainterPath::MoveToElement
//    {
//        case QPainterPath::LineToElement:
////            path.lineTo();
////            path.moveTo(polygons[0])
//            break;
//    }

    QGraphicsPathItem::paint(painter, option, widget);
    painter->drawEllipse(QPoint(10,10), 13, 200);
}
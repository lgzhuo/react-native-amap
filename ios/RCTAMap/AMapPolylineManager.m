//
//  AMapPolylineManager.m
//  Awesome
//
//  Created by yunrui on 2017/3/16.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapPolylineManager.h"
#import "AMapPolyline.h"
#import "RCTConvert+MAMap.h"

@implementation AMapPolylineManager

RCT_EXPORT_MODULE(AMapPolyline)

RCT_EXPORT_VIEW_PROPERTY(coordinates, AMapCoordinateArray)
RCT_EXPORT_VIEW_PROPERTY(strokeColor, UIColor)
RCT_EXPORT_VIEW_PROPERTY(strokeWidth, CGFloat)
RCT_EXPORT_VIEW_PROPERTY(lineCap, MALineCapType)
RCT_EXPORT_VIEW_PROPERTY(lineJoin, MALineJoinType)
RCT_EXPORT_VIEW_PROPERTY(miterLimit, CGFloat)
RCT_EXPORT_VIEW_PROPERTY(lineDash, BOOL)
RCT_EXPORT_VIEW_PROPERTY(onPress, RCTBubblingEventBlock)

- (UIView *)view
{
  AMapPolyline *polyline = [AMapPolyline new];
  return polyline;
}

@end

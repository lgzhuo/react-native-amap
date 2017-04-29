//
//  RCTConvert+MAMap.h
//  Awesome
//
//  Created by yunrui on 2017/3/16.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import <React/RCTConvert+CoreLocation.h>
#import <MAMapKit/MAMapKit.h>
#import <AMapNaviKit/AMapNaviKit.h>
#import "ClusterPoint.h"
#import "AMapCoordinate.h"

@interface RCTConvert (MAMap)
+ (NSValue *)AMapCoordinate:(id)json;
+ (NSArray<NSValue *> *)AMapCoordinateArray:(id)json;
+ (MALineCapType)MALineCapType:(id)json;
+ (MALineJoinType)MALineJoinType:(id)json;
+ (AMapNaviPoint*)AMapNaviPoint:(id)json;
+ (NSArray<AMapNaviPoint*>*)AMapNaviPointArray:(id)json;

+ (ClusterPoint*)ClusterPoint:(id)json;
+ (NSArray<ClusterPoint*>*)ClusterPointArray:(id)json;

+ (MACoordinateSpan)MACoordinateSpan:(id)json;
+ (MACoordinateRegion)MACoordinateRegion:(id)json;

+ (MACoordinateSpan)MACoordinateSpan:(id)json;
+ (MACoordinateRegion)MACoordinateRegion:(id)json;
@end

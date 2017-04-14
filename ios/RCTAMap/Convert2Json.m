//
//  Convert2Json.m
//  Awesome
//
//  Created by yunrui on 2017/3/20.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "Convert2Json.h"
#import "AMapNaviRoute+ID.h"
#import <objc/message.h>

@implementation Convert2Json

NSArray *ConvertArray(SEL type, NSArray *array)
{
  __block BOOL copy = NO;
  __block NSArray *values = array;
  [array enumerateObjectsUsingBlock:^(id object, NSUInteger idx, __unused BOOL *stop) {
    id value = ((id(*)(Class, SEL, id))objc_msgSend)([Convert2Json class], type, object);
    if (copy) {
      if (value) {
        [(NSMutableArray *)values addObject:value];
      }
    } else if (value != object) {
      values = [[NSMutableArray alloc] initWithCapacity:values.count];
      for (NSUInteger i = 0; i < idx; i++) {
        [(NSMutableArray *)values addObject:array[i]];
      }
      if (value) {
        [(NSMutableArray *)values addObject:value];
      }
      copy = YES;
    }
  }];
  return values;
}

+(NSDictionary*)AMapNaviPoint:(AMapNaviPoint*)point{
  return @{@"latitude":@(point.latitude),
           @"longitude":@(point.longitude)};
}

JSON_ARRAY_CONVERTER(AMapNaviPoint)

+(NSDictionary*)AMapNaviLink:(AMapNaviLink*)link{
  return @{@"coordinates":link.coordinates?[self AMapNaviPointArray:link.coordinates]:[NSNull null],
           @"length":@(link.length),
           @"time":@(link.time),
           @"roadName":link.roadName?link.roadName:[NSNull null],
           @"roadType":@(link.formWay),
           @"roadClass":@(link.roadClass),
           @"hasTrafficLights":@(link.isHadTrafficLights)};
}

JSON_ARRAY_CONVERTER(AMapNaviLink)

+(NSDictionary*)AMapNaviPointBounds:(AMapNaviPointBounds*)bounds{
  return @{@"southwest":bounds.southWest?[self AMapNaviPoint:bounds.southWest]:[NSNull null],
           @"northeast":bounds.northEast?[self AMapNaviPoint:bounds.northEast]:[NSNull null]};
}

+(NSDictionary*)AMapNaviSegment:(AMapNaviSegment*)segment{
  return @{@"coordinates":segment.coordinates?[self AMapNaviPointArray:segment.coordinates]:[NSNull null],
           @"length":@(segment.length),
           @"time":@(segment.time),
           @"chargeLength":@(segment.chargeLength),
           @"links":segment.links?[self AMapNaviLinkArray:segment.links]:[NSNull null]};
}

JSON_ARRAY_CONVERTER(AMapNaviSegment)

+(NSArray*)NSIndexPath:(NSIndexPath*)indexPath{
  NSMutableArray *array = [NSMutableArray arrayWithCapacity:indexPath.length];
  for (NSInteger i=0; i<indexPath.length; i++) {
    [array addObject:@([indexPath indexAtPosition:i])];
  }
  return array;
}

+(NSDictionary*)AMapNaviRoute:(AMapNaviRoute*) route{
  
  return @{@"id":@(route.routeID),
           @"length":@(route.routeLength),
           @"time":@(route.routeTime),
           @"bounds":route.routeBounds?[self AMapNaviPointBounds:route.routeBounds]:[NSNull null],
           @"center":route.routeCenterPoint?[self AMapNaviPoint:route.routeCenterPoint]:[NSNull null],
           @"start":route.routeStartPoint?[self AMapNaviPoint:route.routeStartPoint]:[NSNull null],
           @"end":route.routeEndPoint?[self AMapNaviPoint:route.routeEndPoint]:[NSNull null],
           @"strategy":@(route.routeStrategy),
           @"coordinates":route.routeCoordinates?[self AMapNaviPointArray:route.routeCoordinates]:[NSNull null],
           @"tollCost":@(route.routeTollCost),
           @"steps":route.routeSegments?[self AMapNaviSegmentArray:route.routeSegments]:[NSNull null],
           @"stepsCount":@(route.routeSegmentCount),
           @"wayPoints":route.wayPoints?[self AMapNaviPointArray:route.wayPoints]:[NSNull null],
           @"wayPointIndexes":route.wayPointsIndexes?[self NSIndexPath:route.wayPointsIndexes]:[NSNull null]};
}

JSON_ARRAY_CONVERTER(AMapNaviRoute)

+(NSDictionary*)ClusterPoint:(ClusterPoint*) point{
    return @{@"latitude":@(point.latitude),
             @"longitude":@(point.longitude),
             @"id":point.id,
             @"idx":@(point.idx)};
}

JSON_ARRAY_CONVERTER(ClusterPoint)

+(NSDictionary*)CLLocationCoordinate2D:(CLLocationCoordinate2D) coordinate{
    return @{@"latitude":@(coordinate.latitude),
             @"longitude":@(coordinate.longitude)};
}

+(NSDictionary*)Cluster:(Cluster *)cluster{
    return @{@"coordinate":[self CLLocationCoordinate2D:cluster.coordinate],
             @"points":[self ClusterPointArray:cluster.points]};
}

JSON_ARRAY_CONVERTER(Cluster)

@end

//
//  RCTConvert+MAMap.m
//  Awesome
//
//  Created by yunrui on 2017/3/16.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "RCTConvert+MAMap.h"

@implementation RCTConvert (MAMap)

+ (NSValue *)AMapCoordinate:(id)json
{
  CLLocationCoordinate2D coordinate = [self CLLocationCoordinate2D:json];
  return [NSValue valueWithBytes:&coordinate objCType:@encode(CLLocationCoordinate2D)];
}

RCT_ARRAY_CONVERTER_NAMED(NSValue, AMapCoordinate)

RCT_ENUM_CONVERTER(MALineCapType, (@{
                                     @"butt": @(kMALineCapButt),
                                     @"arrow": @(kMALineCapArrow),
                                     @"round": @(kMALineCapRound),
                                     @"square": @(kMALineCapSquare)
                                     }), kMALineCapButt, intValue)

RCT_ENUM_CONVERTER(MALineJoinType, (@{
                                     @"bevel": @(kMALineJoinBevel),
                                     @"miter": @(kMALineJoinMiter),
                                     @"round": @(kMALineJoinRound)
                                     }), kMALineJoinBevel, intValue)

+(AMapNaviPoint*)AMapNaviPoint:(id)json{
  return [AMapNaviPoint locationWithLatitude:[self CGFloat:json[@"latitude"]] longitude:[self CGFloat:json[@"longitude"]]];
}
RCT_ARRAY_CONVERTER(AMapNaviPoint)

+(ClusterPoint*)ClusterPoint:(id)json{
    return [ClusterPoint pointWithLatitude:[self double:json[@"latitude"]] longitude:[self double:json[@"longitude"]] id:[self NSString:@"id"]];
}
RCT_ARRAY_CONVERTER(ClusterPoint)

+ (MACoordinateSpan)MACoordinateSpan:(id)json
{
    json = [self NSDictionary:json];
    return (MACoordinateSpan){
        [self CLLocationDegrees:json[@"latitudeDelta"]],
        [self CLLocationDegrees:json[@"longitudeDelta"]]
    };
}

+ (MACoordinateRegion)MACoordinateRegion:(id)json
{
    return (MACoordinateRegion){
        [self CLLocationCoordinate2D:json],
        [self MACoordinateSpan:json]
    };
}

+ (id)AMapGeoPoint:(id)json{
    return [AMapGeoPoint locationWithLatitude:[self CGFloat:json[@"latitude"]] longitude:[self CGFloat:json[@"longitude"]]];
}
@end

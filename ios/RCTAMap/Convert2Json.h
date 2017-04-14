//
//  Convert2Json.h
//  Awesome
//
//  Created by yunrui on 2017/3/20.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AMapNaviKit/AMapNaviKit.h>
#import "Cluster.h"
#import "ClusterPoint.h"

@interface Convert2Json : NSObject

+(NSDictionary*)AMapNaviRoute:(AMapNaviRoute*) route;
+(NSArray*)AMapNaviRouteArray:(NSArray<AMapNaviRoute*>*) array;

+(NSDictionary*)ClusterPoint:(ClusterPoint*) point;
+(NSArray*)ClusterPointArray:(NSArray<ClusterPoint*>*) array;

+(NSDictionary*)Cluster:(Cluster*)cluster;
+(NSArray*)ClusterArray:(NSArray<Cluster*>*) array;

@end

extern NSArray *ConvertArray(SEL, NSArray* array);

#define JSON_ARRAY_CONVERTER_NAMED(type, name)                    \
+ (NSArray<NSDictionary *> *)name##Array:(NSArray<type *> *)array \
{                                                                 \
  return ConvertArray(@selector(name:), array);                   \
}

#define JSON_ARRAY_CONVERTER(type) JSON_ARRAY_CONVERTER_NAMED(type, type)

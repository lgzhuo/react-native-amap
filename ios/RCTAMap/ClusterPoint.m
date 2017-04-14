//
//  ClusterPoint.m
//  Pods
//
//  Created by yunrui on 2017/4/11.
//
//

#import "ClusterPoint.h"

@implementation ClusterPoint
+(instancetype)pointWithLatitude:(double)latitude longitude:(double)longitude id:(NSString *)id{
    ClusterPoint *point = [[ClusterPoint alloc]init];
    point.latitude = latitude;
    point.longitude = longitude;
    point.id = id;
    return point;
}
@end

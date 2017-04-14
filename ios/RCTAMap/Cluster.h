//
//  Cluster.h
//  Pods
//
//  Created by yunrui on 2017/4/11.
//
//

#import <Foundation/Foundation.h>
#import "ClusterPoint.h"
#import <MAMapKit/MAMapKit.h>

@interface Cluster : NSObject

@property(nonatomic, strong) NSArray<ClusterPoint*> *points;
@property(nonatomic, assign) CLLocationCoordinate2D coordinate;

+(instancetype)clusterWithCoordinate:(CLLocationCoordinate2D) coordinate points:(NSArray<ClusterPoint*>*) points;
@end

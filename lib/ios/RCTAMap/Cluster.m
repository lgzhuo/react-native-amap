//
//  Cluster.m
//  Pods
//
//  Created by yunrui on 2017/4/11.
//
//

#import "Cluster.h"

@implementation Cluster
+(instancetype)clusterWithCoordinate:(CLLocationCoordinate2D)coordinate points:(NSArray<ClusterPoint *> *)points{
    Cluster *cluster = [[Cluster alloc]init];
    cluster.coordinate = coordinate;
    cluster.points = points;
    return cluster;
}
@end

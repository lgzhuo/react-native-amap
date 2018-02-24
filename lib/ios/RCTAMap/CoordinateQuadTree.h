//
//  CoordinateQuadTree.h
//  Pods
//
//  Created by yunrui on 2017/4/11.
//
//

#import <Foundation/Foundation.h>
#import "ClusterPoint.h"
#import "QuadTree.h"
#import "Cluster.h"

@interface CoordinateQuadTree : NSObject
@property (nonatomic, assign) QuadTreeNode * root;

- (void)buildTreeWithPoints:(NSArray<ClusterPoint *> *)pois;
- (void)clean;
- (NSArray<Cluster*> *)clusteredWithinMapRect:(MAMapRect)rect zoomScale:(double)zoomScale size:(float)size;
@end

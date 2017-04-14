//
//  CoordinateQuadTree.m
//  Pods
//
//  Created by yunrui on 2017/4/11.
//
//

#import "CoordinateQuadTree.h"
#import <MAMapKit/MAMapKit.h>

QuadTreeNodeData QuadTreeNodeDataForPoint(ClusterPoint* point)
{
    return QuadTreeNodeDataMake(point.latitude, point.longitude, (__bridge_retained void *)(point));
}

BoundingBox BoundingBoxForMapRect(MAMapRect mapRect)
{
    CLLocationCoordinate2D topLeft = MACoordinateForMapPoint(mapRect.origin);
    CLLocationCoordinate2D botRight = MACoordinateForMapPoint(MAMapPointMake(MAMapRectGetMaxX(mapRect), MAMapRectGetMaxY(mapRect)));
    
    CLLocationDegrees minLat = botRight.latitude;
    CLLocationDegrees maxLat = topLeft.latitude;
    
    CLLocationDegrees minLon = topLeft.longitude;
    CLLocationDegrees maxLon = botRight.longitude;
    
    return BoundingBoxMake(minLat, minLon, maxLat, maxLon);
}

BoundingBox quadTreeNodeDataArrayForPOIs(QuadTreeNodeData *dataArray, NSArray<ClusterPoint*> * pois)
{
    CLLocationDegrees minX = (pois[0]).latitude;
    CLLocationDegrees maxX = (pois[0]).latitude;
    
    CLLocationDegrees minY = (pois[0]).longitude;
    CLLocationDegrees maxY = (pois[0]).longitude;
    
    for (NSInteger i = 0; i < [pois count]; i++)
    {
        dataArray[i] = QuadTreeNodeDataForPoint(pois[i]);
        
        if (dataArray[i].x < minX)
        {
            minX = dataArray[i].x;
        }
        
        if (dataArray[i].x > maxX)
        {
            maxX = dataArray[i].x;
        }
        
        if (dataArray[i].y < minY)
        {
            minY = dataArray[i].y;
        }
        
        if (dataArray[i].y > maxY)
        {
            maxY = dataArray[i].y;
        }
    }
    
    return BoundingBoxMake(minX, minY, maxX, maxY);
}

@implementation CoordinateQuadTree

- (NSArray<Cluster*> *)clusteredWithinMapRect:(MAMapRect)rect zoomScale:(double)zoomScale size:(float)size
{
    double scaleFactor = zoomScale / size;
    
    NSInteger minX = floor(MAMapRectGetMinX(rect) * scaleFactor);
    NSInteger maxX = floor(MAMapRectGetMaxX(rect) * scaleFactor);
    NSInteger minY = floor(MAMapRectGetMinY(rect) * scaleFactor);
    NSInteger maxY = floor(MAMapRectGetMaxY(rect) * scaleFactor);
    
    NSMutableArray *clustered = [[NSMutableArray alloc] init];
    for (NSInteger x = minX; x <= maxX; x++)
    {
        for (NSInteger y = minY; y <= maxY; y++)
        {
            MAMapRect mapRect = MAMapRectMake(x / scaleFactor, y / scaleFactor, 1.0 / scaleFactor, 1.0 / scaleFactor);
            
            __block double totalX = 0;
            __block double totalY = 0;
            __block int     count = 0;
            
            NSMutableArray *points = [[NSMutableArray alloc] init];
            
            /* 查询区域内数据的个数. */
            QuadTreeGatherDataInRange(self.root, BoundingBoxForMapRect(mapRect), ^(QuadTreeNodeData data)
                                      {
                                          totalX += data.x;
                                          totalY += data.y;
                                          count++;
                                          [points addObject:(__bridge ClusterPoint *)data.data];
                                      });
            
            if (count >= 1)
            {
                CLLocationCoordinate2D coordinate = CLLocationCoordinate2DMake(totalX / count, totalY / count);
                [clustered addObject:[Cluster clusterWithCoordinate:coordinate points:points]];
            }
        }
    }
    
    return [NSArray arrayWithArray:clustered];
}

-(void)buildTreeWithPoints:(NSArray<ClusterPoint *> *)pois{
    QuadTreeNodeData *dataArray = malloc(sizeof(QuadTreeNodeData) * [pois count]);
    
    BoundingBox maxBounding = quadTreeNodeDataArrayForPOIs(dataArray, pois);
    
    /*若已有四叉树，清空.*/
    [self clean];
    
    NSLog(@"build tree.");
    /*建立四叉树索引. */
    self.root = QuadTreeBuildWithData(dataArray, [pois count], maxBounding, 4);
    
    free(dataArray);
}

- (void)clean
{
    if (self.root)
    {
        NSLog(@"free tree.");
        FreeQuadTreeNode(self.root);
    }
    
}

@end

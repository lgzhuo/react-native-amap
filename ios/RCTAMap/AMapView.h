//
//  AMapView.h
//  Awesome
//
//  Created by yunrui on 2017/3/13.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import <MAMapKit/MAMapKit.h>
#import <React/RCTComponent.h>
#import "SMCalloutView.h"
#import "ClusterPoint.h"

@interface AMapView : MAMapView
@property(nonatomic,readonly) SMCalloutView *callout;
@property(nonatomic,readonly) NSArray *reactSubviews;
@property(nonatomic,strong) NSArray<ClusterPoint*> *clusterData;
@property(nonatomic,assign) float clusterSize;

@property (nonatomic, copy) RCTDirectEventBlock onCluster;

-(void)calcuteCluster;
@end

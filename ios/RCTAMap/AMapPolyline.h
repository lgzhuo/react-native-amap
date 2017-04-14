//
//  AMapPolyline.h
//  Awesome
//
//  Created by yunrui on 2017/3/16.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import <React/RCTView.h>
#import <MAMapKit/MAMapKit.h>

@interface AMapPolyline : RCTView<MAOverlay>

@property(nonatomic, weak) MAMapView *map;

@property(nonatomic, strong) MAPolyline *polyline;
@property(nonatomic, strong) MAPolylineRenderer *renderer;

@property(nonatomic, assign) NSArray<NSValue*> *coordinates;
@property (nonatomic, strong) UIColor *fillColor;
@property (nonatomic, strong) UIColor *strokeColor;
@property (nonatomic, assign) CGFloat strokeWidth;
@property (nonatomic, assign) CGFloat miterLimit;
@property (nonatomic, assign) MALineCapType lineCap;
@property (nonatomic, assign) MALineJoinType lineJoin;
@property (nonatomic, assign) BOOL lineDash;
@property (nonatomic, copy) RCTBubblingEventBlock onPress;

@end

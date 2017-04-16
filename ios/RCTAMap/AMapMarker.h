//
//  AMapMarker.h
//  Awesome
//
//  Created by yunrui on 2017/3/14.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import <MAMapKit/MAMapKit.h>
#import <React/RCTBridge.h>
#import <React/RCTComponent.h>
#import "AMapView.h"
#import "SMCalloutView.h"

@interface AMapMarker : MAAnnotationView<MAAnnotation>

@property (nonatomic, weak) RCTBridge *bridge;
@property (nonatomic, readonly) MAAnnotationView *annotationView;
@property (nonatomic, weak) AMapView *mapView;

///标注view中心坐标
@property (nonatomic, assign) CLLocationCoordinate2D coordinate;
///annotation标题
@property (nonatomic, copy) NSString *title;
///annotation副标题
@property (nonatomic, copy) NSString *subtitle;

@property (nonatomic, assign) MAPinAnnotationColor pinColor;
@property (nonatomic, copy) NSString *imageSrc;
@property (nonatomic, assign) BOOL calloutEnabled;
@property (nonatomic, copy) RCTDirectEventBlock onSelect;
@property (nonatomic, copy) RCTBubblingEventBlock onPress;
@property (nonatomic, copy) RCTDirectEventBlock onCalloutPress;

- (void)showCallout:(BOOL) animate;
- (void)dismissCallout:(BOOL) animate;
- (void)addTapGestureRecognizer;
@end
@interface EmptyCalloutBackgroundView : SMCalloutBackgroundView
@end

//
//  AMapViewManager.m
//  Awesome
//
//  Created by yunrui on 2017/3/10.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapViewManager.h"
#import "AMapView.h"
#import <React/RCTUIManager.h>
#import "RCTConvert+MAMap.h"
#import "AMapMarker.h"
#import "AMapPolyline.h"
#import "Convert2Json.h"

@implementation Convert2Json(Location)

+(NSDictionary*)CLLocation:(CLLocation*)location{
    return @{
             @"latitude": @(location.coordinate.latitude),
             @"longitude": @(location.coordinate.longitude),
             @"altitude": @(location.altitude),
             @"accuracy": @(location.horizontalAccuracy),
             @"altitudeAccuracy": @(location.verticalAccuracy),
             @"heading": @(location.course),
             @"speed": @(location.speed),
             @"bearing": @(location.course),
             @"timestamp": @([location.timestamp timeIntervalSince1970] * 1000), // in ms
             };
}

@end

@implementation RCTConvert(MapKit)

+ (MKCoordinateSpan)MKCoordinateSpan:(id)json
{
    json = [self NSDictionary:json];
    return (MKCoordinateSpan){
        [self CLLocationDegrees:json[@"latitudeDelta"]],
        [self CLLocationDegrees:json[@"longitudeDelta"]]
    };
}

+ (MKCoordinateRegion)MKCoordinateRegion:(id)json
{
    return (MKCoordinateRegion){
        [self CLLocationCoordinate2D:json],
        [self MKCoordinateSpan:json]
    };
}

RCT_ENUM_CONVERTER(MKMapType, (@{
                                 @"standard": @(MKMapTypeStandard),
                                 @"satellite": @(MKMapTypeSatellite),
                                 @"hybrid": @(MKMapTypeHybrid),
                                 }), MKMapTypeStandard, integerValue)
@end

@interface AMapViewManager ()<MAMapViewDelegate>

@end

@implementation AMapViewManager

RCT_EXPORT_MODULE(AMap)

-(UIView*)view{
  AMapView* mapView = [[AMapView alloc]init];
  mapView.delegate = self;
  return mapView;
}

RCT_EXPORT_VIEW_PROPERTY(showsUserLocation, BOOL)
RCT_EXPORT_VIEW_PROPERTY(showsCompass, BOOL)
RCT_EXPORT_VIEW_PROPERTY(showsScale, BOOL)
RCT_EXPORT_VIEW_PROPERTY(clusterData, ClusterPointArray)
RCT_EXPORT_VIEW_PROPERTY(clusterSize, float)
RCT_EXPORT_VIEW_PROPERTY(onCluster, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(initialRegion, MACoordinateRegion)

RCT_CUSTOM_VIEW_PROPERTY(region, MACoordinateRegion, AMapView)
{
    if (json == nil) return;
    
    [view setRegion:[RCTConvert MACoordinateRegion:json] animated:NO];
}


RCT_EXPORT_METHOD(centerCoordinate: (nonnull NSNumber *)reactTag
                  coordinate:(CLLocationCoordinate2D)coordinate
                  animated:(BOOL)animated){
  [self.bridge.uiManager addUIBlock:
   ^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, AMapView *> *viewRegistry) {
     AMapView *view = viewRegistry[reactTag];
     if (!view || ![view isKindOfClass:[AMapView class]]) {
       RCTLogError(@"Cannot find AMapView with tag #%@", reactTag);
       return;
     }
     [view setCenterCoordinate:coordinate animated:animated];
   }];
}

RCT_EXPORT_METHOD(centerUserLocation: (nonnull NSNumber *)reactTag animated:(BOOL)animated){
  [self.bridge.uiManager addUIBlock:
   ^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, AMapView *> *viewRegistry) {
     AMapView *view = viewRegistry[reactTag];
     if (!view || ![view isKindOfClass:[AMapView class]]) {
       RCTLogError(@"Cannot find AMapView with tag #%@", reactTag);
       return;
     }
     [view setCenterCoordinate:view.userLocation.coordinate animated:animated];
   }];
}

RCT_EXPORT_METHOD(fitAnnotations: (nonnull NSNumber *)reactTag padding:(CGFloat)padding animated:(BOOL)animated){
    [self.bridge.uiManager addUIBlock:
     ^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, AMapView *> *viewRegistry) {
         AMapView *view = viewRegistry[reactTag];
         if (!view || ![view isKindOfClass:[AMapView class]]) {
             RCTLogError(@"Cannot find AMapView with tag #%@", reactTag);
             return;
         }
         NSMutableArray *annotations = [NSMutableArray array];
        [view.reactSubviews enumerateObjectsUsingBlock:^(__kindof UIView * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
            if ([obj conformsToProtocol:@protocol(MAAnnotation)]) {
                [annotations addObject:obj];
            }
        }];
         if (annotations.count) {
             [view showAnnotations:annotations edgePadding:UIEdgeInsetsMake(padding, padding, padding, padding) animated:animated];
         }
     }];

}

RCT_EXPORT_METHOD(fitRegion:(nonnull NSNumber *)reactTag
                  withRegion:(MACoordinateRegion)region
                  animate:(BOOL)animate
                  duration:(CGFloat)duration)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        UIView *view = viewRegistry[reactTag];
        if (![view isKindOfClass:[AMapView class]]) {
            RCTLogError(@"Cannot find AMapView with tag #%@", reactTag);
            return;
        }
        if(animate && duration>0){
            [AMapView animateWithDuration:duration/1000 animations:^{
                [(AMapView*)view setRegion:region animated:YES];
            }];
        }else{
            [(AMapView*)view setRegion:region animated:animate];
        }
    }];
}

RCT_EXPORT_METHOD(fitCoordinates:(nonnull NSNumber *)reactTag
                  coordinates:(nonnull id)coordinates
                  edgePadding:(NSDictionary *)edgePadding
                  animated:(BOOL)animated)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        UIView *view = viewRegistry[reactTag];
        if (![view isKindOfClass:[AMapView class]]) {
            RCTLogError(@"Cannot find AMapView with tag #%@", reactTag);
            return;
        }
        
        // Create Polyline with coordinates
        NSArray<NSValue*> *coordinateArray = [RCTConvert AMapCoordinateArray:coordinates];
        CLLocationCoordinate2D coords[coordinateArray.count];
        for(int i = 0; i < coordinateArray.count; i++)
        {
            [coordinateArray[i] getValue:&coords[i]];
        }
        MAPolyline *polyline = [MAPolyline polylineWithCoordinates:coords count:coordinateArray.count];
        
        CGFloat top = [RCTConvert CGFloat:edgePadding[@"top"]];
        CGFloat right = [RCTConvert CGFloat:edgePadding[@"right"]];
        CGFloat bottom = [RCTConvert CGFloat:edgePadding[@"bottom"]];
        CGFloat left = [RCTConvert CGFloat:edgePadding[@"left"]];
        
        [(AMapView*)view setVisibleMapRect:polyline.boundingMapRect edgePadding:UIEdgeInsetsMake(top, left, bottom, right) animated:animated];
    }];
}

#pragma mark MAMapViewDelegate

/**
 * @brief 根据anntation生成对应的View
 * @param mapView 地图View
 * @param annotation 指定的标注
 * @return 生成的标注View
 */
- (MAAnnotationView *)mapView:(AMapView *)mapView viewForAnnotation:(AMapMarker*)annotation{
  if ([annotation isKindOfClass:[AMapMarker class]]) {
    annotation.bridge = self.bridge;
    annotation.mapView = mapView;
    return annotation.annotationView;
  }
  return nil;
}

-(void)mapView:(MAMapView *)mapView didSelectAnnotationView:(MAAnnotationView *)view{
  if ([view.annotation isKindOfClass:[AMapMarker class]]) {
    [(AMapMarker*)view.annotation showCallout:YES];
  }
}

-(void)mapView:(MAMapView *)mapView didDeselectAnnotationView:(MAAnnotationView *)view{
  if ([view.annotation isKindOfClass:[AMapMarker class]]) {
    [(AMapMarker*)view.annotation dismissCallout:YES];
  }
}

-(MAOverlayRenderer *)mapView:(MAMapView *)mapView rendererForOverlay:(id<MAOverlay>)overlay{
  if ([overlay isKindOfClass:[AMapPolyline class]]) {
    return ((AMapPolyline *)overlay).renderer;
  }else{
    return nil;
  }
}

- (void)mapView:(MAMapView *)mapView regionDidChangeAnimated:(BOOL)animated{
    [(AMapView*)mapView calcuteCluster];
}

- (void)mapView:(AMapView *)mapView didUpdateUserLocation:(MAUserLocation *)userLocation updatingLocation:(BOOL)updatingLocation{
    if (updatingLocation && mapView.onLocationUpdate && userLocation.location) {
        mapView.onLocationUpdate(@{@"location":[Convert2Json CLLocation:userLocation.location]});
    }
}

@end

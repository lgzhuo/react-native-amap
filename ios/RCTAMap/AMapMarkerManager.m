//
//  AMapMarkerManager.m
//  Awesome
//
//  Created by yunrui on 2017/3/14.
//  Copyright © 2017年 Facebook. All rights reserved.
//

#import "AMapMarkerManager.h"
#import "AMapMarker.h"
#import <React/RCTConvert+CoreLocation.h>
#import <React/RCTUIManager.h>

@implementation AMapMarkerManager

RCT_EXPORT_MODULE(AMapMarker)

-(UIView*)view{
    AMapMarker *view = [[AMapMarker alloc]initWithAnnotation:nil reuseIdentifier:nil];
    //  view.annotation = view;
    [view addTapGestureRecognizer];
    view.bridge = self.bridge;
    
    return view;
}

RCT_EXPORT_VIEW_PROPERTY(coordinate, CLLocationCoordinate2D)
RCT_EXPORT_VIEW_PROPERTY(title, NSString)
RCT_REMAP_VIEW_PROPERTY(description, subtitle, NSString)
RCT_EXPORT_VIEW_PROPERTY(draggable, BOOL)
RCT_CUSTOM_VIEW_PROPERTY(pinColor, UIColor, AMapMarker){
  UIColor *color = [RCTConvert UIColor:json];
  CGFloat hue;
  if([color getHue:&hue saturation:nil brightness:nil alpha:nil]){
    if(hue < 1/3) {
      view.pinColor = MAPinAnnotationColorRed;
    } else if(hue < 2/3) {
      view.pinColor = MAPinAnnotationColorGreen;
    } else {
      view.pinColor = MAPinAnnotationColorPurple;
    }
  }
}
RCT_REMAP_VIEW_PROPERTY(image, imageSrc, NSString)
RCT_EXPORT_VIEW_PROPERTY(centerOffset, CGPoint)
RCT_EXPORT_VIEW_PROPERTY(calloutOffset, CGPoint)
RCT_EXPORT_VIEW_PROPERTY(zIndex, NSInteger)
RCT_EXPORT_VIEW_PROPERTY(calloutEnabled, BOOL)
RCT_EXPORT_VIEW_PROPERTY(onSelect, RCTDirectEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onPress, RCTBubblingEventBlock)
RCT_EXPORT_VIEW_PROPERTY(onCalloutPress, RCTDirectEventBlock)

RCT_EXPORT_METHOD(showCallout:(nonnull NSNumber *)reactTag)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        if (![view isKindOfClass:[AMapMarker class]]) {
            RCTLogError(@"Invalid view returned from registry, expecting AMapMarker, got: %@", view);
        } else {
            [(AMapMarker *) view showCallout:YES];
        }
    }];
}

RCT_EXPORT_METHOD(hideCallout:(nonnull NSNumber *)reactTag)
{
    [self.bridge.uiManager addUIBlock:^(__unused RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        id view = viewRegistry[reactTag];
        if (![view isKindOfClass:[AMapMarker class]]) {
            RCTLogError(@"Invalid view returned from registry, expecting AMapMarker, got: %@", view);
        } else {
            [(AMapMarker *) view dismissCallout:YES];
        }
    }];
}

@end
